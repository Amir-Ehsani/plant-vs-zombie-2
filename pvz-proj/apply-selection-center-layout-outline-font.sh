#!/usr/bin/env bash
set -e

git apply selection-center-layout-outline-font.patch
git add core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java \
        core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java
git commit -m "ui: center plant selection layout"
