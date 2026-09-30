import { useEffect, useState } from "react"
import type { EventEnvelope } from "../models/event";
import { WEBSOCKET_ENDPOINT } from "../constants/routes";

export type EventSubscription = {
    data: EventEnvelope | null,
    hasError: boolean,
    isConnected: boolean
}

const useEventSubscription = (): EventSubscription => {
    const [eventEnvelope, setEventEnvelope] = useState<EventEnvelope | null>(null)
    const [isConnected, setConnected] = useState<boolean>(false)
    const [hasError, setError] = useState<boolean>(false)

    useEffect(() => {
        const websocket = new WebSocket(WEBSOCKET_ENDPOINT);
        websocket.onopen = () => {
            console.log(`Event websocket connected on: ${WEBSOCKET_ENDPOINT}`);
            setConnected(true);
        }

        websocket.onclose = () => {
            console.log(`Event websocket closed on: ${WEBSOCKET_ENDPOINT}`);
            setConnected(false);
        }

        websocket.onerror = (e) => {
            console.log("Error from event websocket");
            setError(true)
        }

        websocket.onmessage = (event) => {
            const data: EventEnvelope | undefined = JSON.parse(event.data);
            if (typeof data?.tick === 'number') {
                setEventEnvelope(data);

            }
        }

        return () => {
            websocket.close();
        }
    }, []);

    return {
        data: eventEnvelope,
        isConnected,
        hasError
    };
}

export default useEventSubscription;