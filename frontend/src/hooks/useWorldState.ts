import { useReducer } from "react";
import useEventSubscription from "./useEventSubscription";
import { initialWorldState, worldStateReducer } from "../state/worldStateReducer";

import type { WorldState } from "../state/worldStateReducer";

export type { WorldState };

const useWorldState = (): WorldState => {
  const [state, dispatch] = useReducer(worldStateReducer, initialWorldState);

  useEventSubscription(dispatch);

  return state;
};

export default useWorldState;
