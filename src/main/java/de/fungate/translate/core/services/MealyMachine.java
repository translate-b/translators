package de.fungate.translate.core.services;

import fj.F;
import fj.P2;

import java.util.HashMap;
import java.util.Map;

/**
 * Models a finite state machine after the mealy model. For usage, see WoerterbuchTranslator.extractTranslations().
 * @author Eike Karsten Schlicht
 */
public class MealyMachine<TState extends Enum<TState>, TInput, TOutput> {

    private final Map<TState, F<TInput, P2<TState, TOutput>>> handlers;
    private TState state;

    private MealyMachine(TState initial, Map<TState, F<TInput, P2<TState, TOutput>>> handlers) {
        this.state = initial;
        this.handlers = handlers;
    }

    public TState getState() {
        return state;
    }

    /**
     * Checks, if the set of transitions from which the machine is formed is total.
     * @return true, iff all possible enum member of TState are covered by a transition.
     */
    public boolean isTotal() {
        for (TState s : state.getDeclaringClass().getEnumConstants()) {
            if (!handlers.containsKey(s)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Advance the machine one step with the given input. May result in a different state and
     * returns the output of the respective transition.
     * @param input handed to the appropriate transition.
     * @return the output of the fired transition.
     */
    public TOutput step(TInput input) {
        P2<TState, TOutput> output = handlers.get(state).f(input);
        state = output._1();
        return output._2();
    }

    /**
     * Builds a new MealyMachine from an initial state and a number of transitions, best built with the help of
     * when().then() builder methods.
     * @param initial initial state.
     * @param transitions handling the different states.
     * @param <TState> Type of the state. Has to be an enum type.
     * @param <TInput> Type of the input of transitions.
     * @param <TOutput> Type of the ouput of transitions.
     * @return a MealyMachine in the initial state and with the defined transitions.
     */
    @SafeVarargs
    public static <TState extends Enum<TState>, TInput, TOutput>
    MealyMachine<TState, TInput, TOutput> fromTransitions(TState initial, Transition<TState, TInput, TOutput>... transitions) {
        Map<TState, F<TInput, P2<TState, TOutput>>> handlers = new HashMap<>(transitions.length);
        for (Transition<TState, TInput, TOutput> t : transitions) {
            handlers.put(t.getState(), t.getHandler());
        }
        return new MealyMachine<>(initial, handlers);
    }

    /**
     * Starts a Builder chain for defining a new Transition.
     * @param state state of the machine in which the transition will fire.
     * @param <TState> type of the state which the machine will be built on. Must be an enum.
     * @return a new TransitionBuilder meant as an intermediate result to declaratively construct a new Transition.
     * See the (new school) Builder pattern.
     */
    public static <TState extends Enum<TState>> TransitionBuilder<TState> when(TState state) {
        return new TransitionBuilder<>(state);
    }

    public static class TransitionBuilder<TState extends Enum<TState>> {
        private final TState state;

        public TransitionBuilder(TState state) {
            this.state = state;
        }

        /**
         * Creates a new transition.
         * @param handler will be called with an input value and returns an output value and a new state.
         * @param <TInput> Type of input of the transition.
         * @param <TOutput> Type of output of the transition.
         * @return the new transition
         */
        public <TInput, TOutput> Transition<TState, TInput, TOutput> then(F<TInput, P2<TState, TOutput>> handler) {
            return new Transition<>(state, handler);
        }
    }

    /**
     * Represents a transition in the MealyMachine.
     * @param <TState> Type of the state when the Transition will be called.
     * @param <TInput> Type of input of the machine.
     * @param <TOutput> Type of the output of the machine.
     */
    public static class Transition<TState, TInput, TOutput> {
        private final TState state;
        private final F<TInput, P2<TState, TOutput>> handler;

        public Transition(TState state, F<TInput, P2<TState, TOutput>> handler) {
            this.state = state;
            this.handler = handler;
        }

        public TState getState() {
            return state;
        }

        public F<TInput, P2<TState, TOutput>> getHandler() {
            return handler;
        }
    }
}
