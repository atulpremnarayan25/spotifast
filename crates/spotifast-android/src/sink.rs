//! Android audio sink for librespot.
//! Delivers 16-bit 44.1kHz stereo PCM buffers to Android's AudioTrack.

use std::sync::Arc;
use std::sync::atomic::{AtomicU32, Ordering};
use parking_lot::Mutex;

pub type PcmCallback = Arc<dyn Fn(&[i16]) + Send + Sync + 'static>;

pub struct AndroidAudioSink {
    callback: Option<PcmCallback>,
    volume: Arc<AtomicU32>, // 0..65535 cubic scale
}

impl AndroidAudioSink {
    pub fn new(callback: Option<PcmCallback>, volume: Arc<AtomicU32>) -> Self {
        Self { callback, volume }
    }

    pub fn write_pcm(&self, samples: &[i16]) {
        if let Some(ref cb) = self.callback {
            // Apply volume if needed
            let vol = self.volume.load(Ordering::Relaxed) as f32 / 65535.0;
            if (vol - 1.0).abs() < 0.001 {
                cb(samples);
            } else {
                let scaled: Vec<i16> = samples
                    .iter()
                    .map(|&s| ((s as f32) * vol).clamp(-32768.0, 32767.0) as i16)
                    .collect();
                cb(&scaled);
            }
        }
    }
}
