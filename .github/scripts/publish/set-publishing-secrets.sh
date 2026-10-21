#!/usr/bin/env bash
# Sets the four repository secrets the Publish workflow needs. Run it yourself, in a terminal: it prompts for every value
# and prints none, and the values go only to GitHub (and the public half of the signing key to keyserver.ubuntu.com).
#
#   SONATYPEUSERNAME, SONATYPEPASSWORD  a Central Portal user token: sign in at https://central.sonatype.com with GitHub,
#                                       then Account > Generate User Token
#   GPG_PRIVATE_KEY, PASSPHRASE         a new signing key, created here with the passphrase you choose
#
# Usage: .github/scripts/publish/set-publishing-secrets.sh   (REPO=owner/name to target another repository)
set -euo pipefail

REPO=${REPO:-corneloaie/firebase-multiplatform}
KEY_NAME=${KEY_NAME:-"Cornel Oaie (firebase-multiplatform release signing)"}

command -v gh >/dev/null || { echo "Install the GitHub CLI first: brew install gh"; exit 1; }
command -v gpg >/dev/null || brew install gnupg

echo "1/2  Central Portal user token for $REPO"
echo "     (https://central.sonatype.com > Account > Generate User Token)"
gh secret set SONATYPEUSERNAME -R "$REPO"
gh secret set SONATYPEPASSWORD -R "$REPO"

echo "2/2  Signing key \"$KEY_NAME\""
read -r -s -p "Passphrase for the new key: " passphrase; echo
read -r -s -p "The same passphrase again: " again; echo
[ -n "$passphrase" ] && [ "$passphrase" = "$again" ] || { echo "The passphrases are empty or differ."; exit 1; }
fingerprint=$(gpg --batch --pinentry-mode loopback --passphrase "$passphrase" --status-fd 1 \
  --quick-generate-key "$KEY_NAME" rsa4096 sign 0 | awk '/KEY_CREATED/ { print $4 }')
[ -n "$fingerprint" ] || { echo "Creating the key failed."; exit 1; }
gpg --keyserver hkps://keyserver.ubuntu.com --send-keys "$fingerprint"
gpg --batch --pinentry-mode loopback --passphrase "$passphrase" --armor --export-secret-keys "$fingerprint" |
  gh secret set GPG_PRIVATE_KEY -R "$REPO"
printf '%s' "$passphrase" | gh secret set PASSPHRASE -R "$REPO"

echo
echo "Done. Signing key $fingerprint is in your local GnuPG keyring and its public half on keyserver.ubuntu.com"
echo "(Maven Central may take a few minutes to see it). The secrets of $REPO are now:"
gh secret list -R "$REPO"
