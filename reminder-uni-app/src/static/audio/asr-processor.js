/**
 * AudioWorklet-based audio processor for real-time speech recognition.
 * This processor runs in a separate thread to handle audio data efficiently.
 *
 * Responsibilities:
 * 1. Receives raw audio data (Float32Array) from the microphone.
 * 2. Converts the audio data to 16-bit PCM format.
 * 3. Buffers and chunks the data into fixed-size frames (e.g., 1280 bytes).
 * 4. Posts the resulting ArrayBuffer chunks back to the main thread.
 */
class AsrProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    // The frameSize (in Int16 samples) is passed from the main thread.
    // 1280 bytes = 640 Int16 samples.
    this.frameSize = options.processorOptions.frameSize || 640; 
    this.buffer = new Int16Array(this.frameSize * 2); // A buffer to hold more data
    this.bufferIndex = 0;
    this.port.onmessage = (event) => {
      // Handle messages from the main thread if needed, e.g., to stop processing.
    };
  }

  /**
   * Converts a Float32Array to a 16-bit PCM Int16Array.
   * The input values are in the range of -1.0 to 1.0.
   * @param {Float32Array} input
   * @returns {Int16Array}
   */
  floatTo16BitPCM(input) {
    const output = new Int16Array(input.length);
    for (let i = 0; i < input.length; i++) {
      const s = Math.max(-1, Math.min(1, input[i]));
      output[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
    }
    return output;
  }

  /**
   * The main processing function, called by the browser's audio engine.
   */
  process(inputs, outputs, parameters) {
    // We only care about the first input, and its first channel.
    const input = inputs[0];
    if (!input || input.length === 0 || !input[0]) {
      return true; // Keep the processor alive
    }
    const channelData = input[0];

    // Convert the Float32 data to 16-bit PCM.
    const pcmData = this.floatTo16BitPCM(channelData);
    
    // Add the new data to our buffer.
    if (this.bufferIndex + pcmData.length > this.buffer.length) {
      // Buffer overflow, discard old data. This is a fallback.
      console.warn('ASR Processor buffer overflow, discarding data.');
      this.bufferIndex = 0;
    }
    this.buffer.set(pcmData, this.bufferIndex);
    this.bufferIndex += pcmData.length;
    
    // If we have enough data in the buffer, send it to the main thread.
    while (this.bufferIndex >= this.frameSize) {
      const frame = this.buffer.slice(0, this.frameSize);
      
      // Post the data as an ArrayBuffer. The second argument makes it a "transferable" object,
      // which is more efficient as it transfers ownership rather than copying.
      this.port.postMessage(frame.buffer, [frame.buffer]);

      // Shift the remaining data to the start of the buffer.
      this.buffer.copyWithin(0, this.frameSize, this.bufferIndex);
      this.bufferIndex -= this.frameSize;
    }

    return true; // Indicate that the processor should remain active.
  }
}

try {
  registerProcessor('asr-processor', AsrProcessor);
} catch (e) {
  console.error('Failed to register AsrProcessor', e);
} 