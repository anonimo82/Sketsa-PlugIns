package kiyut.sketsa.modules.audio;

/** Neutral names shared by Sketsa authoring modules and exported runtimes. */
public final class AudioRuntimeContract {
    public static final String CONTRACT_VERSION = "1.0";
    public static final String EVENT_READY = "sketsa:audio:ready";
    public static final String EVENT = "sketsa:audio:event";
    public static final String ACTION = "sketsa:audio:action";
    public static final String ACTION_RESULT = "sketsa:audio:actionResult";
    public static final String RUNTIME_EVENT = "sketsa:runtime:event";
    public static final String SOURCE_PHYSICS = "physics";
    public static final String PHYSICS_COLLISION_START = "collisionStart";

    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_RESTART = "restart";
    public static final String ACTION_SET_VOLUME = "setVolume";
    public static final String ACTION_SET_MUTE = "setMute";
    public static final String ACTION_SET_PLAYBACK_RATE = "setPlaybackRate";
    public static final String ACTION_SET_LOOP = "setLoop";
    public static final String ACTION_SET_PAN = "setPan";
    public static final String ACTION_SET_PAN_MODE = "setPanMode";
    public static final String ACTION_SET_BUS = "setBus";
    public static final String ACTION_SET_BUS_VOLUME = "setBusVolume";
    public static final String ACTION_SET_BUS_MUTE = "setBusMute";
    public static final String ACTION_SET_MASTER_VOLUME = "setMasterVolume";
    public static final String ACTION_SET_MASTER_MUTE = "setMasterMute";
    public static final String ACTION_RESUME = "resume";

    private AudioRuntimeContract() {}
}
