package com.novasonic.app

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

private const val TWO_PI = 6.2831855f
private const val BLOCK = 32               // control-rate: angle/gains are updated every 32 frames
private const val DLY_SIZE = 256
private const val DLY_MASK = DLY_SIZE - 1
private const val REVERB_INPUT = 0.22f
private const val REVERB_FEEDBACK = 0.82f
private const val REVERB_DAMP = 0.25f
private val COMB_TUNING = intArrayOf(1116, 1188, 1277, 1356)
private val ALLPASS_TUNING = intArrayOf(556, 441)

private class Comb(size: Int) {
    private val buf = FloatArray(size)
    private var idx = 0
    private var store = 0f
    fun process(input: Float): Float {
        val out = buf[idx]
        store = out * (1f - REVERB_DAMP) + store * REVERB_DAMP
        buf[idx] = input + store * REVERB_FEEDBACK
        if (++idx >= buf.size) idx = 0
        return out
    }
    fun clear() { buf.fill(0f); idx = 0; store = 0f }
}

private class AllPass(size: Int) {
    private val buf = FloatArray(size)
    private var idx = 0
    fun process(input: Float): Float {
        val b = buf[idx]
        val out = b - input
        buf[idx] = input + b * 0.5f
        if (++idx >= buf.size) idx = 0
        return out
    }
    fun clear() { buf.fill(0f); idx = 0 }
}

private fun softClip(x: Float): Float = when {
    x > 0.95f -> 0.95f + 0.05f * tanh((x - 0.95f) / 0.05f)
    x < -0.95f -> -0.95f - 0.05f * tanh((-x - 0.95f) / 0.05f)
    else -> x
}

/**
 * Real-time 8D / binaural "orbit" effect.
 *
 *  - The centre (mid) signal is moved around the listener. Bass (< ~180 Hz) stays centred, because
 *    low frequencies are not localisable and panning them just sounds wobbly.
 *  - Interaural time difference (Woodworth model, up to ~0.65 ms) delays the far ear.
 *  - Interaural level difference + a head-shadow low-pass darken the far ear.
 *  - When the source is behind you it gets a pinna-style high-frequency roll-off (front/back cue).
 *  - The original stereo difference (side) is kept as a quieter ambient layer.
 *  - A small stereo reverb pushes the sound out of your head.
 *  - Turning it on/off cross-fades over ~150 ms (no clicks); when off it is bit-exact pass-through.
 *
 * Parameters are @Volatile and may be written from any thread; all DSP state lives on the audio thread.
 */
@OptIn(UnstableApi::class)
class EightDAudioProcessor : BaseAudioProcessor() {

    @Volatile var enabled: Boolean = false
    @Volatile var mode: Int = EightD.MODE_ORBIT
    @Volatile var intensity: Float = 0.85f
    @Volatile var periodSeconds: Float = 8.8f

    private var sampleRate = 44100
    private var inChannels = 2
    private var isFloat = false
    private var inFrameBytes = 4
    private var outFrameBytes = 4

    // reverb
    private var combsL: Array<Comb> = emptyArray()
    private var combsR: Array<Comb> = emptyArray()
    private var allL: Array<AllPass> = emptyArray()
    private var allR: Array<AllPass> = emptyArray()

    // spatialiser state
    private val dly = FloatArray(DLY_SIZE)
    private var dlyPos = 0
    private var lowState = 0f
    private var lpL = 0f
    private var lpR = 0f

    // control values (refreshed every BLOCK frames)
    private var counter = 0
    private var phase = 0.0
    private var mix = 0f
    private var mixTarget = 0f
    private var mixCoef = 0.0005f
    private var lowCoef = 0.97f
    private var delayL = 0f
    private var delayR = 0f
    private var gainL = 1f
    private var gainR = 1f
    private var coefL = 0f
    private var coefR = 0f
    private var sideGain = 0.55f
    private var reverbLevel = 0.2f
    private var bypassed = true

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        val ch = inputAudioFormat.channelCount
        // Anything we can't handle is simply left untouched (processor inactive) instead of failing playback.
        if ((enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) || (ch != 1 && ch != 2)) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        inChannels = ch
        isFloat = enc == C.ENCODING_PCM_FLOAT
        val bytes = if (isFloat) 4 else 2
        inFrameBytes = ch * bytes
        outFrameBytes = 2 * bytes

        val scale = sampleRate / 44100f
        fun size(base: Int) = max(8, (base * scale).toInt())
        combsL = Array(COMB_TUNING.size) { Comb(size(COMB_TUNING[it])) }
        combsR = Array(COMB_TUNING.size) { Comb(size(COMB_TUNING[it] + 23)) }
        allL = Array(ALLPASS_TUNING.size) { AllPass(size(ALLPASS_TUNING[it])) }
        allR = Array(ALLPASS_TUNING.size) { AllPass(size(ALLPASS_TUNING[it] + 23)) }
        mixCoef = 1f - exp(-1f / (0.15f * sampleRate))
        lowCoef = exp(-TWO_PI * 180f / sampleRate)
        clearState()
        return AudioProcessor.AudioFormat(sampleRate, 2, enc)
    }

    override fun onFlush() {
        clearState()
    }

    private fun clearState() {
        dly.fill(0f)
        dlyPos = 0
        lowState = 0f
        lpL = 0f
        lpR = 0f
        for (c in combsL) c.clear()
        for (c in combsR) c.clear()
        for (a in allL) a.clear()
        for (a in allR) a.clear()
        counter = 0
    }

    /** Recomputes the (slowly changing) spatial parameters. Called once per BLOCK frames. */
    private fun updateControl() {
        val on = enabled
        val m = mode
        mixTarget = if (on) intensity.coerceIn(0f, 1f) else 0f
        if (mixTarget <= 0f && mix <= 0f) return

        val period = periodSeconds.coerceIn(2f, 60f)
        val theta = when (m) {
            EightD.MODE_SWEEP -> 1.3f * sin(TWO_PI * phase.toFloat())
            EightD.MODE_SPATIAL -> 0f
            else -> TWO_PI * phase.toFloat()
        }
        if (m != EightD.MODE_SPATIAL) {
            phase += BLOCK / (period.toDouble() * sampleRate)
            if (phase >= 1.0) phase -= 1.0
        }

        val lat = sin(theta)                 // -1 = hard left, +1 = hard right
        val front = cos(theta)               //  1 = front, -1 = behind
        val absLat = min(abs(lat), 1f)
        val latAngle = asin(absLat)
        val itdSamples = 0.000255f * (latAngle + sin(latAngle)) * sampleRate
        delayL = if (lat > 0f) itdSamples else 0f
        delayR = if (lat < 0f) itdSamples else 0f

        val farGain = 10f.pow(-6f * absLat / 20f)
        val nearGain = 10f.pow(1.5f * absLat / 20f)
        val back = max(0f, -front)
        val backGain = 10f.pow(-2.5f * back / 20f)
        gainL = (if (lat > 0f) farGain else nearGain) * backGain
        gainR = (if (lat > 0f) nearGain else farGain) * backGain

        val fcFar = 18000f * 0.1f.pow(absLat)                       // 18 kHz -> 1.8 kHz head shadow
        val fcBack = 18000f * (4000f / 18000f).pow(back)            // 18 kHz -> 4 kHz when behind
        val cap = 0.45f * sampleRate
        val fcL = min(if (lat > 0f) fcFar else 18000f, fcBack)
        val fcR = min(if (lat < 0f) fcFar else 18000f, fcBack)
        coefL = exp(-TWO_PI * min(fcL, cap) / sampleRate)
        coefR = exp(-TWO_PI * min(fcR, cap) / sampleRate)

        if (m == EightD.MODE_SPATIAL) {
            sideGain = 1.4f
            reverbLevel = 0.30f
        } else {
            sideGain = 0.55f
            reverbLevel = 0.20f
        }
    }

    private fun readDelay(d: Float): Float {
        val di = d.toInt()
        val frac = d - di
        val i0 = (dlyPos - 1 - di) and DLY_MASK
        val i1 = (i0 - 1) and DLY_MASK
        return dly[i0] * (1f - frac) + dly[i1] * frac
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val frames = inputBuffer.remaining() / inFrameBytes
        if (frames <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val out = replaceOutputBuffer(frames * outFrameBytes)

        for (n in 0 until frames) {
            if (counter == 0) updateControl()
            counter = (counter + 1) and (BLOCK - 1)

            val rawL: Short
            val rawR: Short
            val l: Float
            val r: Float
            if (isFloat) {
                rawL = 0
                rawR = 0
                l = inputBuffer.getFloat()
                r = if (inChannels == 2) inputBuffer.getFloat() else l
            } else {
                rawL = inputBuffer.getShort()
                rawR = if (inChannels == 2) inputBuffer.getShort() else rawL
                l = rawL / 32768f
                r = rawR / 32768f
            }

            mix += (mixTarget - mix) * mixCoef
            if (mixTarget == 0f && mix < 1e-4f) mix = 0f

            if (mix == 0f) {
                // fully off: exact pass-through
                bypassed = true
                if (isFloat) {
                    out.putFloat(l)
                    out.putFloat(r)
                } else {
                    out.putShort(rawL)
                    out.putShort(rawR)
                }
                continue
            }
            if (bypassed) {
                clearState()
                counter = 1 // keep the parameters just computed for this block
                bypassed = false
            }

            val mid = 0.5f * (l + r)
            val side = 0.5f * (l - r)

            // bass stays centred, only the rest is moved around the head
            lowState += (1f - lowCoef) * (mid - lowState)
            val high = mid - lowState
            dly[dlyPos] = high
            dlyPos = (dlyPos + 1) and DLY_MASK

            val sL = readDelay(delayL)
            val sR = readDelay(delayR)
            lpL += (1f - coefL) * (sL - lpL)
            lpR += (1f - coefR) * (sR - lpR)
            val eL = lpL * gainL + lowState
            val eR = lpR * gainR + lowState

            val rin = 0.5f * (eL + eR) * REVERB_INPUT
            var rvL = 0f
            var rvR = 0f
            for (c in combsL) rvL += c.process(rin)
            for (c in combsR) rvR += c.process(rin)
            for (a in allL) rvL = a.process(rvL)
            for (a in allR) rvR = a.process(rvR)

            val wetL = eL + side * sideGain + rvL * reverbLevel
            val wetR = eR - side * sideGain + rvR * reverbLevel
            val oL = softClip(l * (1f - mix) + wetL * mix)
            val oR = softClip(r * (1f - mix) + wetR * mix)

            if (isFloat) {
                out.putFloat(oL)
                out.putFloat(oR)
            } else {
                out.putShort((oL * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
                out.putShort((oR * 32767f).roundToInt().coerceIn(-32768, 32767).toShort())
            }
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }
}
