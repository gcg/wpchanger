.DEFAULT_GOAL := help
.PHONY: help open run build assemble build-release install test lint format check clean

# Ensure Android SDK platform-tools & emulator are in PATH
export PATH := $(HOME)/Library/Android/sdk/platform-tools:$(HOME)/Library/Android/sdk/emulator:$(PATH)

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

build: ## Build debug APK
	@./gradlew assembleDebug

assemble: build ## Alias for build

build-release: ## Build release APK
	@./gradlew assembleRelease

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
