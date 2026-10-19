import { useEffect, useRef, useState } from "react";
import type { WebsocketsEvent } from "../models/event";
import { WEBSOCKET_ENDPOINT } from "../constants/routes";

export type EventSubscription = {
  data: WebsocketsEvent | null;
  hasError: boolean;
  isConnected: boolean;
};

export type EventCallback = (event: WebsocketsEvent) => void;

const parseEvent = (raw: string): WebsocketsEvent | undefined => {
  try {
    const parsed = JSON.parse(raw) as WebsocketsEvent | undefined;
    return typeof parsed?.tick === "number" ? parsed : undefined;
  } catch {
    return undefined;
  }
};

const useEventSubscription = (onEvent?: EventCallback): EventSubscription => {
  const [event, setEvent] = useState<WebsocketsEvent | null>(null);
  const [isConnected, setConnected] = useState<boolean>(false);
  const [hasError, setHasError] = useState<boolean>(false);
  const onEventRef = useRef(onEvent);

  useEffect(() => {
    onEventRef.current = onEvent;
  }, [onEvent]);

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
      const data = parseEvent(event.data);
      if (data !== undefined) {
        setEvent(data);
        onEventRef.current?.(data);
      }
    };

    return () => {
      websocket.close();
    };
  }, []);

  return {
    data: event,
    isConnected,
    hasError,
  };
};

export default useEventSubscription;
