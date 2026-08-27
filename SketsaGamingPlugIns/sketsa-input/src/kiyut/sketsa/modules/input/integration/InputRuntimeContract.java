package kiyut.sketsa.modules.input.integration;

public final class InputRuntimeContract {
    public static final String CONTRACT_VERSION="1.0";
    public static final String READY_EVENT="sketsa:input:ready";
    public static final String INPUT_EVENT="sketsa:input:event";
    public static final String ACTION_EVENT="sketsa:input:action";
    public static final String ACTION_RESULT_EVENT="sketsa:input:actionResult";
    public static final String RUNTIME_EVENT="sketsa:runtime:event";
    public static final String PHYSICS_ACTION_EVENT="sketsa:physics:action";
    public static final String PHYSICS_ACTION_RESULT_EVENT="sketsa:physics:actionResult";
    public static final String POINTER_DEVICE="pointer";
    public static final String GAMEPAD_DEVICE="gamepad";
    public static final String ONSCREEN_DEVICE="onscreen";
    public static final String KEYBOARD_DEVICE="keyboard";
    public static final String ACTION_GET_STATE="getState";
    public static final String ACTION_GET_SNAPSHOT="getSnapshot";
    public static final String ACTION_RESET="resetAction";
    public static final String ACTION_RESET_ALL="resetAll";
    private InputRuntimeContract() {}
}
