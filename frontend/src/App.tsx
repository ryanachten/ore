import useWorldState from "./hooks/useWorldState";

function App() {
  const state = useWorldState();

  return (
    <section id="center">
      <div>
        <h1>Tick: {state.tick ?? "Unknown"}</h1>
        <ul>
          {Object.keys(state.vehicles).map((id) => (
            <li
              key={id}
            >{`state: ${state.vehicles[id].state}, x: ${state.vehicles[id].position.x}, y: ${state.vehicles[id].position.y}`}</li>
          ))}
        </ul>
      </div>
    </section>
  );
}

export default App;
