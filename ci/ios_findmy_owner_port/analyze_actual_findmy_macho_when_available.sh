#!/usr/bin/env bash
# OPTIONAL NATIVE REA/GHIDRA ON VERIFIED LOCAL ORIGINAL APPLE MACH-O.
# No Apple original executable was available in the completed Oct17 CI.
# This script will REFUSE source-diff text, mocks, IPA containers or bad SHA.
# It never downloads a proprietary binary or publishes native pseudocode.
set -euo pipefail
if [[ $# -ne 3 ]]; then
  echo "USAGE: $0 /absolute/path/to/FindMy <64-char-sha256> /private/output_dir" >&2
  exit 2
fi
target="$1"; expected="$2"; out="$3"
[[ -f "$target" && -s "$target" ]] || { echo "NATIVE_MACHO=NOT_PROVIDED" >&2; exit 4; }
[[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { echo "invalid sha256" >&2; exit 4; }
[[ "$(sha256sum "$target" | cut -d' ' -f1)" = "$expected" ]] || {
  echo "APPLE_MACHO_SHA256_MISMATCH" >&2; exit 4;
}
# Thin arm64 Mach-O 0xcffaedfe, or universal FAT header; text never admitted.
magic="$(od -An -tx1 -N4 "$target" | tr -d ' \n')"
case "$magic" in cffaedfe|cafebabe|cafebabf|bebafeca|bfbafeca) ;; *)
  echo "NOT_APPLE_MACHO_HEADER=$magic" >&2;exit 4;;
esac
[[ ! -e "$out" ]] || { echo "output dir already exists: $out" >&2;exit 4; }
mkdir -m 700 "$out"
chmod 700 "$out"
# JDK21+ Ghidra 12.1.x and the REA 6.5.0 native provider MUST be installed.
# Returned raw evidence remains in owner-only files, never public CI artifacts.
rea doctor --provider ghidra --json > "$out/ghidra-doctor-private.json"
rea analyze "$target" --provider ghidra --json > "$out/real-native-overview-private.json"
rea search "$target" "startPlaySound" --provider ghidra --json > "$out/sound-symbols-private.json"
rea search "$target" "FMFindingExperienceCoordinator" --provider ghidra --json > "$out/coordinator-private.json"
echo "REAL_APPLE_MACHO_REA_GHIDRA_ANALYSIS=RAN"
echo "SOURCE_SHA256=$expected"
echo "READ_ONLY=YES"
echo "ACTUAL_A3529_CASE_RING_STILL_REQUIRES_AUTH_SESSION_AND_COMMAND_PROOF"
