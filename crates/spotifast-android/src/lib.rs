//! JNI entry points for Spotifast Android Native Librespot Bridge.

pub mod sink;
pub mod engine;

use std::sync::Arc;
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jlong, jstring};
use crate::engine::LibrespotEngine;

static mut ENGINE_INSTANCE: Option<Arc<LibrespotEngine>> = None;

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeInit(
    _env: JNIEnv,
    _class: JClass,
    _cache_dir: JString,
) -> jboolean {
    #[cfg(target_os = "android")]
    android_logger::init_once(
        android_logger::Config::default()
            .with_max_level(log::LevelFilter::Debug)
            .with_tag("SpotifastNative"),
    );

    match LibrespotEngine::new() {
        Ok(engine) => {
            unsafe {
                ENGINE_INSTANCE = Some(Arc::new(engine));
            }
            1
        }
        Err(e) => {
            log::error!("Failed to initialize native engine: {:?}", e);
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeConnect(
    mut env: JNIEnv,
    _class: JClass,
    auth_token: JString,
) -> jboolean {
    let _token_str: String = match env.get_string(&auth_token) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };
    log::info!("Connecting librespot with playback token");
    1
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativePlay(
    _env: JNIEnv,
    _class: JClass,
) {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.play();
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativePause(
    _env: JNIEnv,
    _class: JClass,
) {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.pause();
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeSeek(
    _env: JNIEnv,
    _class: JClass,
    position_ms: jlong,
) {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.seek(position_ms as u64);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeLoadTrack(
    mut env: JNIEnv,
    _class: JClass,
    uri: JString,
) {
    let uri_str: String = match env.get_string(&uri) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.load_track(uri_str);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeSetVolume(
    _env: JNIEnv,
    _class: JClass,
    volume: jint,
) {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.set_volume(volume as u32);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeGetPositionMs(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.position_ms() as jlong
        } else {
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeGetDurationMs(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            engine.duration_ms() as jlong
        } else {
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeIsPlaying(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    unsafe {
        if let Some(ref engine) = ENGINE_INSTANCE {
            if engine.is_playing() { 1 } else { 0 }
        } else {
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_rocks_spotifast_native_LibrespotBridge_nativeRelease(
    _env: JNIEnv,
    _class: JClass,
) {
    unsafe {
        ENGINE_INSTANCE = None;
    }
}
