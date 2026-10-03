.DEFAULT_GOAL := help
.PHONY: help open run run-device devices build assemble build-release bundle keystore install test lint format check clean

# Ensure Android SDK platform-tools & emulator are in PATH
export PATH := $(HOME)/Library/Android/sdk/platform-tools:$(HOME)/Library/Android/sdk/emulator:$(PATH)

# Referenced by absolute path (not just via PATH) so device-targeting targets below are robust
# even in shells where the PATH export above doesn't propagate to every subprocess.
ADB := $(HOME)/Library/Android/sdk/platform-tools/adb

PACKAGE_NAME := com.gcg.wpchanger
ACTIVITY_NAME := $(PACKAGE_NAME)/.MainActivity

help: ## Show available commands and their descriptions
	@echo "\033[1;36mWallpaper Changer - Project Commands\033[0m"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[32m%-16s\033[0m %s\n", $$1, $$2}'
	@echo ""

open: ## Open project in Android Studio
	@echo "Opening project in Android Studio..."
	@if [ -d "/Applications/Android Studio.app" ]; then \
		open -a "/Applications/Android Studio.app" . ; \
	elif which studio >/dev/null 2>&1; then \
		studio . ; \
	else \
		open -a "Android Studio" . || echo "Android Studio not found. Please open manually."; \
	fi

run: build ## Run app in connected device or launch emulator first
	@echo "Checking for running devices/emulators..."
	@if ! adb get-state >/dev/null 2>&1; then \
		echo "No active Android device detected. Checking available emulators..."; \
		AVD=$$(emulator -list-avds 2>/dev/null | head -n 1); \
		if [ -z "$$AVD" ]; then \
			echo "Error: No Android Virtual Device (AVD) found. Please create one in Android Studio Device Manager."; \
			exit 1; \
		fi; \
		echo "Starting AVD: $$AVD..."; \
		emulator -avd "$$AVD" -no-boot-anim & \
		echo "Waiting for emulator to boot up..."; \
		adb wait-for-device; \
		while [ "$$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do \
			sleep 2; \
		done; \
		echo "Emulator booted successfully!"; \
	fi
	@echo "Installing and launching app on device..."
	@./gradlew installDebug
	@echo "Starting $(ACTIVITY_NAME)..."
	@adb shell am start -n $(ACTIVITY_NAME)

devices: ## List adb-visible devices/emulators with their state
	@$(ADB) devices -l

run-device: build ## Install and launch on a connected physical phone (never an emulator)
	@echo "Looking for a connected physical Android device..."
	@DEVICE=$$($(ADB) devices | tail -n +2 | grep -w "device" | grep -v "^emulator-" | head -n1 | cut -f1); \
	if [ -z "$$DEVICE" ]; then \
		echo "\033[1;31mNo physical device found via adb.\033[0m"; \
		echo ""; \
		echo "  1. Enable Developer Options on the phone (Settings > About phone > tap Build number 7x)"; \
		echo "  2. Enable USB debugging (Settings > System > Developer options)"; \
		echo "  3. Connect via USB cable, or 'adb pair'/'adb connect' for wireless debugging"; \
		echo "  4. Accept the 'Allow USB debugging?' prompt on the phone"; \
		echo "  5. Run 'make devices' to confirm it shows up as 'device' (not 'unauthorized')"; \
		exit 1; \
	fi; \
	echo "Found device: $$DEVICE"; \
	export ANDROID_SERIAL=$$DEVICE; \
	./gradlew installDebug; \
	echo "Starting $(ACTIVITY_NAME) on $$DEVICE..."; \
	$(ADB) -s $$DEVICE shell am start -n $(ACTIVITY_NAME)

build: ## Build debug APK
	@./gradlew assembleDebug

assemble: build ## Alias for build

build-release: ## Build release APK
	@./gradlew assembleRelease

bundle: ## Build the signed release App Bundle (.aab) for Google Play
	@if [ ! -f keystore.properties ] && [ -z "$$WPCHANGER_STORE_FILE" ]; then \
		echo "\033[1;31mNo signing config found.\033[0m Run 'make keystore', then copy keystore.properties.example to keystore.properties."; \
		exit 1; \
	fi
	@./gradlew bundleRelease
	@echo "\033[1;32mBundle ready:\033[0m app/build/outputs/bundle/release/app-release.aab"

keystore: ## Generate a Play Store upload key (upload-keystore.jks, git-ignored)
	@if [ -f upload-keystore.jks ]; then echo "upload-keystore.jks already exists - refusing to overwrite."; exit 1; fi
	@keytool -genkeypair -v -keystore upload-keystore.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000
	@echo "\033[1;33mBack up upload-keystore.jks and its password somewhere safe (e.g. a password manager).\033[0m"

install: ## Install debug APK to connected device/emulator
	@./gradlew installDebug

test: ## Run unit test suite
	@./gradlew test

format: ## Format Kotlin and Gradle source files with Spotless (ktlint)
	@./gradlew spotlessApply

lint: ## Run Spotless format checks and Android Lint
	@./gradlew spotlessCheck lintDebug

check: format lint test ## Run formatter, linter, and unit tests (Best practices CI check)
	@echo "\033[1;32mAll quality checks passed successfully!\033[0m"

clean: ## Clean build outputs and caches
	@./gradlew clean
