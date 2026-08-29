#!/usr/bin/env bash
set -e

git apply selection-sun-cost-yellow.patch
git add core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java \
        core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java
git commit -m "style: make plant selection sun cost yellow"
