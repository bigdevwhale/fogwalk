"""Makes demo/assets/demo_walks.json: three weeks of walks along real streets for `-Pdemo` screenshots.

Streets come from OpenStreetMap through the Overpass API. Walks are random strolls on the street graph
that prefer unvisited streets, so together they chart a believable patch of the city.

    python tools/demo_walks.py
"""
import json
import math
import random
import urllib.parse
import urllib.request
from collections import defaultdict
from pathlib import Path

# Prague's Old Town and Malá Strana: old streets, a river, bridges.
CENTER = (50.0868, 14.4155)
BBOX = (50.0800, 14.3990, 50.0935, 14.4320)
WALKS = 18
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "demo" / "assets" / "demo_walks.json"

QUERY = f"""
[out:json][timeout:60];
way["highway"~"^(footway|pedestrian|residential|living_street|tertiary|secondary|unclassified|path|steps)$"]({BBOX[0]},{BBOX[1]},{BBOX[2]},{BBOX[3]});
(._;>;);
out body;
"""


def dist(a, b):
    dy = (a[0] - b[0]) * 111_320
    dx = (a[1] - b[1]) * 111_320 * math.cos(math.radians(a[0]))
    return math.hypot(dx, dy)


def main():
    req = urllib.request.Request(
        "https://overpass.kumi.systems/api/interpreter",
        data=urllib.parse.urlencode({"data": QUERY}).encode(),
        headers={"User-Agent": "fogwalk-demo/1.0"},
    )
    data = json.load(urllib.request.urlopen(req, timeout=120))
    nodes = {e["id"]: (e["lat"], e["lon"]) for e in data["elements"] if e["type"] == "node"}
    graph = defaultdict(set)
    for e in data["elements"]:
        if e["type"] != "way":
            continue
        ns = [n for n in e["nodes"] if n in nodes]
        for a, b in zip(ns, ns[1:]):
            graph[a].add(b)
            graph[b].add(a)

    rnd = random.Random(7)
    used = defaultdict(int)
    start = min(graph, key=lambda n: dist(nodes[n], CENTER))
    walks = []
    for w in range(WALKS):
        # Each walk begins somewhere already walked, so the charted area grows outward.
        here = start if w == 0 else rnd.choice([n for walk in walks for n in walk])
        prev = None
        path = [here]
        length = 0.0
        goal = rnd.uniform(1200, 3200)
        while length < goal:
            options = [n for n in graph[here] if n != prev] or list(graph[here])
            if not options:
                break
            weights = [1.0 / (1 + 3 * used[(min(here, n), max(here, n))]) for n in options]
            nxt = rnd.choices(options, weights)[0]
            used[(min(here, nxt), max(here, nxt))] += 1
            length += dist(nodes[here], nodes[nxt])
            prev, here = here, nxt
            path.append(here)
        walks.append(path)

    out = {
        "lat": nodes[walks[-1][-1]][0],
        "lon": nodes[walks[-1][-1]][1],
        "walks": [[[round(nodes[n][0], 6), round(nodes[n][1], 6)] for n in walk] for walk in walks],
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(out, separators=(",", ":")))
    print(f"{len(walks)} walks, {sum(len(w) for w in walks)} points -> {OUT}")


if __name__ == "__main__":
    main()
