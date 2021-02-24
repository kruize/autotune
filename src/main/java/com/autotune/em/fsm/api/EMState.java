package com.autotune.em.fsm.api;

public class EMState {

    private final String name;

    /**
     * Create a new {@link EMState}.
     *
     * @param name of the state
     */
    public EMState(final String name) {
        this.name = name;
    }

    /**
     * Get state name.
     * @return state name
     */
    public String getName() {
        return name;
    }

    /*
     * States have unique name within a Easy States FSM instance
     */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EMState state = (EMState) o;

        return name.equals(state.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
