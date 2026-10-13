//! Librespot core audio engine and session management.

use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};
use parking_lot::Mutex;
use tokio::runtime::Runtime;
use tokio::sync::mpsc;

#[derive(Clone, Debug, serde::Serialize)]
pub struct PlaybackStateDto {
    pub is_playing: bool,
    pub position_ms: u64,
    pub duration_ms: u64,
    pub track_uri: String,
    pub track_name: String,
    pub artist_name: String,
    pub album_art_url: String,
}

pub enum Command {
    Play,
    Pause,
    Seek(u64),
    LoadTrack(String),
    SetVolume(u32),
    Stop,
}

pub struct LibrespotEngine {
    runtime: Arc<Runtime>,
    command_tx: mpsc::UnboundedSender<Command>,
    is_playing: Arc<AtomicBool>,
    position_ms: Arc<AtomicU64>,
    duration_ms: Arc<AtomicU64>,
    volume: Arc<AtomicU32>,
}

impl LibrespotEngine {
    pub fn new() -> Result<Self, anyhow::Error> {
        let runtime = Arc::new(Runtime::new()?);
        let (command_tx, mut command_rx) = mpsc::unbounded_channel::<Command>();
        let is_playing = Arc::new(AtomicBool::new(false));
        let position_ms = Arc::new(AtomicU64::new(0));
        let duration_ms = Arc::new(AtomicU64::new(0));
        let volume = Arc::new(AtomicU32::new(65535));

        let is_playing_clone = Arc::clone(&is_playing);
        let position_clone = Arc::clone(&position_ms);

        // Background actor loop
        runtime.spawn(async move {
            while let Some(cmd) = command_rx.recv().await {
                match cmd {
                    Command::Play => {
                        is_playing_clone.store(true, Ordering::SeqCst);
                    }
                    Command::Pause => {
                        is_playing_clone.store(false, Ordering::SeqCst);
                    }
                    Command::Seek(pos) => {
                        position_clone.store(pos, Ordering::SeqCst);
                    }
                    Command::LoadTrack(_uri) => {
                        position_clone.store(0, Ordering::SeqCst);
                        is_playing_clone.store(true, Ordering::SeqCst);
                    }
                    Command::SetVolume(_vol) => {
                        // volume updated
                    }
                    Command::Stop => {
                        is_playing_clone.store(false, Ordering::SeqCst);
                        break;
                    }
                }
            }
        });

        Ok(Self {
            runtime,
            command_tx,
            is_playing,
            position_ms,
            duration_ms,
            volume,
        })
    }

    pub fn play(&self) {
        let _ = self.command_tx.send(Command::Play);
    }

    pub fn pause(&self) {
        let _ = self.command_tx.send(Command::Pause);
    }

    pub fn seek(&self, position_ms: u64) {
        let _ = self.command_tx.send(Command::Seek(position_ms));
    }

    pub fn load_track(&self, uri: String) {
        let _ = self.command_tx.send(Command::LoadTrack(uri));
    }

    pub fn set_volume(&self, volume: u32) {
        self.volume.store(volume, Ordering::Relaxed);
        let _ = self.command_tx.send(Command::SetVolume(volume));
    }

    pub fn is_playing(&self) -> bool {
        self.is_playing.load(Ordering::Relaxed)
    }

    pub fn position_ms(&self) -> u64 {
        self.position_ms.load(Ordering::Relaxed)
    }

    pub fn duration_ms(&self) -> u64 {
        self.duration_ms.load(Ordering::Relaxed)
    }
}
