import { useEffect, useState } from "react";
import type { EventEnvelope } from "../models/event";
import { WEBSOCKET_ENDPOINT } from "../constants/routes";

export type EventSubscription = {
  data: EventEnvelope | null;
  hasError: boolean;
  isConnected: boolean;
};

const parseEnvelope = (raw: string): EventEnvelope | undefined => {
  try {
    const parsed = JSON.parse(raw) as EventEnvelope | undefined;
    return typeof parsed?.tick === "number" ? parsed : undefined;
  } catch {
    return undefined;
  }
};

const useEventSubscription = (): EventSubscription => {
  const [eventEnvelope, setEventEnvelope] = useState<EventEnvelope | null>(null);
  const [isConnected, setConnected] = useState<boolean>(false);
  const [hasError, setHasError] = useState<boolean>(false);

  useEffect(() => {
    const websocket = new WebSocket(WEBSOCKET_ENDPOINT);
    websocket.onopen = () => {
      console.log(`Event websocket connected on: ${WEBSOCKET_ENDPOINT}`);
      setConnected(true);
    };

    websocket.onclose = () => {
      console.log(`Event websocket closed on: ${WEBSOCKET_ENDPOINT}`);
      setConnected(false);
    };

    websocket.onerror = () => {
      console.log("Error from event websocket");
      setHasError(true);
    };

    websocket.onmessage = (event) => {
      const data = parseEnvelope(event.data);
      if (data !== undefined) {
        setEventEnvelope(data);
      }
    };

    return () => {
      websocket.close();
    };
  }, []);

  return {
    data: eventEnvelope,
    isConnected,
    hasError,
  };
};

export default useEventSubscription;
