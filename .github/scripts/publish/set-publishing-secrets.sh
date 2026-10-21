#!/usr/bin/env bash
# Sets the four repository secrets the Publish workflow needs. Run it yourself, in a terminal: it prompts for every value
# and prints none, and the values go only to GitHub (and the public half of the signing key to the keyservers).
#
#   SONATYPEUSERNAME, SONATYPEPASSWORD  a Central Portal user token: sign in at https://central.sonatype.com with GitHub,
#                                       then Account > Generate User Token, and use its username and password (not the
#                                       base64 form of both)
#   GPG_PRIVATE_KEY, PASSPHRASE         the signing key named $KEY_NAME: an existing one is reused (it asks for its
#                                       passphrase), otherwise a new one is created with the passphrase you choose
#
# It can be run again: token secrets that are set already are kept unless FORCE=1, and the key is reused.
# Usage: .github/scripts/publish/set-publishing-secrets.sh   (REPO=owner/name to target another repository)
set -euo pipefail

REPO=${REPO:-corneloaie/firebase-multiplatform}
KEY_NAME=${KEY_NAME:-"Cornel Oaie (firebase-multiplatform release signing)"}

command -v gh >/dev/null || { echo "Install the GitHub CLI first: brew install gh"; exit 1; }
command -v gpg >/dev/null || brew install gnupg

existing=$(gh secret list -R "$REPO" --json name --jq '.[].name')
if [ "${FORCE:-0}" != 1 ] && grep -qx SONATYPEUSERNAME <<<"$existing" && grep -qx SONATYPEPASSWORD <<<"$existing"; then
  echo "1/2  SONATYPEUSERNAME and SONATYPEPASSWORD are set already (FORCE=1 replaces them)."
else
  echo "1/2  Central Portal user token for $REPO: paste its username, then its password"
  echo "     (https://central.sonatype.com > Account > Generate User Token)"
  gh secret set SONATYPEUSERNAME -R "$REPO"
  gh secret set SONATYPEPASSWORD -R "$REPO"
fi

fingerprint=$(gpg --list-secret-keys --with-colons "$KEY_NAME" 2>/dev/null | awk -F: '/^fpr:/ { print $10; exit }' || true)
if [ -n "$fingerprint" ]; then
  echo "2/2  Signing key $fingerprint (\"$KEY_NAME\")"
  read -r -s -p "Its passphrase: " passphrase; echo
else
  echo "2/2  A new signing key \"$KEY_NAME\""
  read -r -s -p "Passphrase for the new key: " passphrase; echo
  read -r -s -p "The same passphrase again: " again; echo
  [ -n "$passphrase" ] && [ "$passphrase" = "$again" ] || { echo "The passphrases are empty or differ."; exit 1; }
  fingerprint=$(gpg --batch --pinentry-mode loopback --passphrase "$passphrase" --status-fd 1 \
    --quick-generate-key "$KEY_NAME" rsa4096 sign 0 | awk '/KEY_CREATED/ { print $4 }')
  [ -n "$fingerprint" ] || { echo "Creating the key failed."; exit 1; }
fi

# Exporting the secret key needs its passphrase, so this also checks the passphrase.
private_key=$(gpg --batch --pinentry-mode loopback --passphrase "$passphrase" --armor --export-secret-keys "$fingerprint" 2>/dev/null || true)
[ -n "$private_key" ] || { echo "Exporting the key failed: is the passphrase right?"; exit 1; }

# Maven Central looks the public key up on these keyservers. gpg --send-keys fails with some GnuPG versions
# ("Invalid argument"), so the key goes over their HTTPS upload APIs.
public_key=$(gpg --armor --export "$fingerprint")
curl -fsS --data-urlencode "keytext=$public_key" https://keyserver.ubuntu.com/pks/add >/dev/null
python3 -c 'import json, sys; print(json.dumps({"keytext": sys.stdin.read()}))' <<<"$public_key" |
  curl -fsS -H "Content-Type: application/json" -d @- https://keys.openpgp.org/vks/v1/upload >/dev/null

printf '%s' "$private_key" | gh secret set GPG_PRIVATE_KEY -R "$REPO"
printf '%s' "$passphrase" | gh secret set PASSPHRASE -R "$REPO"

echo
echo "Done. Signing key $fingerprint is in your local GnuPG keyring, and its public half on keyserver.ubuntu.com and"
echo "keys.openpgp.org. The secrets of $REPO are now:"
gh secret list -R "$REPO"
