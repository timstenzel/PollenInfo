#!/usr/bin/env bash
# Prepares a fresh Ubuntu 26.04 server for the production stack (deploy/README.md, "Provisioning").
#
#   scp deploy/provision.sh root@<server>:
#   ssh -t root@<server> bash provision.sh [--user <name>]
#
# Run as root. <name> (default tim) becomes the operator: a sudo user that logs in with root's
# authorized SSH key and runs compose. Then:
#
#   - SSH: no root login, no password or keyboard-interactive login — a drop-in that sorts first
#     (00-polleninfo.conf), since sshd keeps the first value it reads and cloud-init's own drop-in
#     may switch passwords on
#   - ufw: OpenSSH, 80/tcp, 443/tcp, 443/udp, everything else incoming denied
#   - fail2ban's sshd jail on the systemd journal
#   - unattended security upgrades, rebooting at 04:00 when an upgrade needs it; the time zone is
#     Europe/Zurich, so that is a Swiss 04:00, the alarms' quietest hour
#   - Docker Engine and the Compose plugin from Docker's apt repository, json-file logs capped by
#     default in /etc/docker/daemon.json
#   - a 2 GB swap file
#   - /opt/polleninfo/secrets (root, for install-secrets.sh) and /opt/polleninfo/deploy (the user)
#
# Idempotent: run again, it changes nothing that is already in place and ends with "No change".
set -euo pipefail

user=tim
while [[ $# -gt 0 ]]; do
    case "$1" in
        --user) user="${2:?--user needs a name}"; shift 2 ;;
        -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
        *) echo "Unknown argument $1" >&2; exit 2 ;;
    esac
done

die() { echo "provision: $*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || die "run as root"
# shellcheck source=/dev/null
. /etc/os-release
[[ "${ID:-}" == ubuntu ]] || die "expected Ubuntu, found ${PRETTY_NAME:-an unknown system}"

export DEBIAN_FRONTEND=noninteractive NEEDRESTART_MODE=a
apt_options=(-q -y -o DPkg::Lock::Timeout=300)

changes=()
changed() {
    changes+=("$1")
    echo "changed: $1"
}

# Writes stdin to $1 (root-owned, mode $2) unless it already holds exactly that. Succeeds only when
# it wrote, so a caller can restart what reads the file.
put_file() {
    local target="$1" mode="$2" tmp
    tmp="$(mktemp)"
    cat >"$tmp"
    if [[ -f "$target" ]] && cmp -s "$tmp" "$target" && [[ "$(stat -c '%a %u' "$target")" == "$mode 0" ]]; then
        rm -f "$tmp"
        return 1
    fi
    install -D -m "$mode" -o 0 -g 0 "$tmp" "$target"
    rm -f "$tmp"
    changed "$target"
}

# Installs whichever of the packages are missing.
apt_install() {
    local missing=() package
    for package in "$@"; do
        dpkg-query -W -f='${Status}' "$package" 2>/dev/null | grep -q 'install ok installed' ||
            missing+=("$package")
    done
    [[ ${#missing[@]} -gt 0 ]] || return 0
    apt-get "${apt_options[@]}" update
    apt-get "${apt_options[@]}" install "${missing[@]}"
    changed "installed ${missing[*]}"
}

enable_now() {
    local unit="$1"
    if ! systemctl is-enabled --quiet "$unit" || ! systemctl is-active --quiet "$unit"; then
        systemctl enable --now "$unit"
        changed "service $unit enabled and started"
    fi
}

ensure_dir() {
    local dir="$1" owner="$2" mode="$3"
    if [[ ! -d "$dir" || "$(stat -c '%U %a' "$dir")" != "$owner $mode" ]]; then
        install -d -o "$owner" -g "$owner" -m "$mode" "$dir"
        changed "$dir"
    fi
}

in_group() { id -nG "$1" | tr ' ' '\n' | grep -qx "$2"; }

apt_install ca-certificates curl openssl ufw fail2ban python3-systemd unattended-upgrades

# --- Time zone -----------------------------------------------------------------------------------

if [[ "$(timedatectl show -p Timezone --value)" != Europe/Zurich ]]; then
    timedatectl set-timezone Europe/Zurich
    changed "time zone Europe/Zurich"
fi

# --- The operator --------------------------------------------------------------------------------

if ! id "$user" >/dev/null 2>&1; then
    adduser --disabled-password --comment "" "$user"
    changed "user $user"
fi
in_group "$user" sudo || { usermod -aG sudo "$user"; changed "$user in group sudo"; }

home="$(getent passwd "$user" | cut -d: -f6)"
keys="$home/.ssh/authorized_keys"
if [[ ! -s "$keys" ]]; then
    [[ -s /root/.ssh/authorized_keys ]] ||
        die "neither $keys nor /root/.ssh/authorized_keys holds a key; add one before root login is switched off"
    install -d -m 0700 -o "$user" -g "$user" "$home/.ssh"
    install -m 0600 -o "$user" -g "$user" /root/.ssh/authorized_keys "$keys"
    changed "$keys (from root's)"
fi

# The user logs in by key only; the password is for sudo.
if [[ "$(passwd -S "$user" | cut -d' ' -f2)" != P ]]; then
    if [[ -t 0 ]]; then
        echo "Choose a password for $user — sudo asks for it; SSH never does."
        passwd "$user"
        changed "password of $user"
    else
        echo "warning: $user has no password, so sudo will not work. Run: passwd $user" >&2
    fi
fi

# --- SSH -----------------------------------------------------------------------------------------

ssh_dropin=/etc/ssh/sshd_config.d/00-polleninfo.conf
if put_file "$ssh_dropin" 644 <<'CONF'
# deploy/provision.sh — sorts first, so these values win over any later drop-in.
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
CONF
then
    install -d -m 0755 /run/sshd # sshd -t needs it; with socket activation it may not exist yet
    sshd -t || { rm -f "$ssh_dropin"; die "sshd rejected $ssh_dropin; removed it again"; }
    systemctl restart ssh
fi

# --- Firewall ------------------------------------------------------------------------------------

ufw_before="$(ufw status verbose 2>/dev/null || true)"
ufw default deny incoming >/dev/null
ufw default allow outgoing >/dev/null
ufw allow OpenSSH >/dev/null
ufw allow 80/tcp >/dev/null
ufw allow 443/tcp >/dev/null
ufw allow 443/udp >/dev/null
ufw status | grep -q '^Status: active' || ufw --force enable >/dev/null
[[ "$(ufw status verbose)" == "$ufw_before" ]] || changed "firewall (ufw)"

# --- fail2ban ------------------------------------------------------------------------------------

if put_file /etc/fail2ban/jail.d/polleninfo.local 644 <<'CONF'
# deploy/provision.sh
[sshd]
enabled = true
backend = systemd
CONF
then
    systemctl restart fail2ban
fi
enable_now fail2ban

# --- Automatic security updates ------------------------------------------------------------------

put_file /etc/apt/apt.conf.d/20auto-upgrades 644 <<'CONF' || true
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
CONF
put_file /etc/apt/apt.conf.d/52polleninfo-unattended-upgrades 644 <<'CONF' || true
// deploy/provision.sh — reboot when an upgrade needs it, at 04:00 (Europe/Zurich).
Unattended-Upgrade::Automatic-Reboot "true";
Unattended-Upgrade::Automatic-Reboot-Time "04:00";
CONF
enable_now unattended-upgrades

# --- Docker --------------------------------------------------------------------------------------

docker_repo=https://download.docker.com/linux/ubuntu
keyring=/etc/apt/keyrings/docker.asc
if [[ ! -s "$keyring" ]]; then
    install -d -m 0755 /etc/apt/keyrings
    curl -fsSL "$docker_repo/gpg" -o "$keyring"
    chmod 0644 "$keyring"
    changed "$keyring"
fi
if [[ ! -f /etc/apt/sources.list.d/docker.sources ]]; then
    curl -fsSI "$docker_repo/dists/$VERSION_CODENAME/Release" >/dev/null ||
        die "Docker publishes no '$VERSION_CODENAME' suite yet; see deploy/README.md, \"Provisioning\""
fi
put_file /etc/apt/sources.list.d/docker.sources 644 <<SOURCES || true
Types: deb
URIs: $docker_repo
Suites: $VERSION_CODENAME
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: $keyring
SOURCES
apt_install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Caps every container's log, also one started by hand; compose.yaml sets the same per service.
if put_file /etc/docker/daemon.json 644 <<'JSON'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
JSON
then
    systemctl restart docker
fi
enable_now docker
in_group "$user" docker || { usermod -aG docker "$user"; changed "$user in group docker"; }

# --- Swap ----------------------------------------------------------------------------------------

if [[ ! -f /swapfile ]]; then
    fallocate -l 2G /swapfile
    chmod 0600 /swapfile
    mkswap /swapfile >/dev/null
    changed "/swapfile (2 GB)"
fi
if ! swapon --show=NAME --noheadings | grep -qx /swapfile; then
    swapon /swapfile
    changed "swap on /swapfile"
fi
grep -q '^/swapfile[[:space:]]' /etc/fstab || {
    echo '/swapfile none swap sw 0 0' >>/etc/fstab
    changed "/etc/fstab: /swapfile"
}

# --- The stack's directories ---------------------------------------------------------------------

ensure_dir /opt/polleninfo root 755
ensure_dir /opt/polleninfo/secrets root 755
ensure_dir /opt/polleninfo/deploy "$user" 755

# -------------------------------------------------------------------------------------------------

if [[ ${#changes[@]} -eq 0 ]]; then
    echo "No change: the server was already provisioned."
else
    echo
    echo "Provisioned (${#changes[@]} changes). Before closing this session, check in a new terminal:"
    echo "  ssh $user@<server>        must work (key), and"
    echo "  ssh root@<server>         must be refused."
fi
