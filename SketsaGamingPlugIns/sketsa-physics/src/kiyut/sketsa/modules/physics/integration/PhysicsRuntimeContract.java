package kiyut.sketsa.modules.physics.integration;

/**
 * Stable names used by exported Sketsa Physics runtimes and other Sketsa plugins.
 * Matter.js types are deliberately not part of this contract.
 */
public final class PhysicsRuntimeContract {
    public static final String CONTRACT_VERSION = "1.0";
    public static final String RUNTIME_VERSION = "0.9.6";

    public static final String EVENT_READY = "sketsa:physics:ready";
    public static final String EVENT_GENERIC = "sketsa:physics:event";
    public static final String EVENT_INTEROP = "sketsa:runtime:event";
    public static final String EVENT_ACTION = "sketsa:physics:action";
    public static final String EVENT_ACTION_RESULT = "sketsa:physics:actionResult";
    public static final String EVENT_COLLISION_START = "sketsa:physics:collisionStart";
    public static final String EVENT_COLLISION_ACTIVE = "sketsa:physics:collisionActive";
    public static final String EVENT_COLLISION_END = "sketsa:physics:collisionEnd";
    public static final String EVENT_COLLISION = "sketsa:physics:collision";

    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_RESET = "reset";
    public static final String ACTION_SLEEP = "sleep";
    public static final String ACTION_WAKE = "wake";
    public static final String ACTION_SET_POSITION = "setPosition";
    public static final String ACTION_SET_ANGLE = "setAngle";
    public static final String ACTION_SET_VELOCITY = "setVelocity";
    public static final String ACTION_SET_ANGULAR_VELOCITY = "setAngularVelocity";
    public static final String ACTION_SET_SLEEPING = "setSleeping";
    public static final String ACTION_APPLY_FORCE = "applyForce";
    public static final String ACTION_APPLY_IMPULSE = "applyImpulse";

    private PhysicsRuntimeContract() {}
}
