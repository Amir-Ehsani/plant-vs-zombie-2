#!/usr/bin/env bash
set -e

git apply selection-sun-cost-reference-style.patch
git add core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java \
        core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java
git commit -m "style: match plant selection sun cost font"
