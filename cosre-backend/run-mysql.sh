#!/usr/bin/env bash
# Luon dung script nay (thay vi go tay tung dong export) de khong bao gio quen
# bat nham profile "dev" (H2, mat du lieu khi tat app) thay vi "mysql".
#
# Cach dung:
#   1. cp .env.example .env   (chi lam 1 lan, roi sua gia tri trong .env cho dung)
#   2. ./run-mysql.sh
set -e
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Khong thay file .env — chay: cp .env.example .env  roi sua lai gia tri cho dung."
  exit 1
fi

echo "Dang doc bien moi truong tu .env ..."
set -a
source .env
set +a

export SPRING_PROFILES_ACTIVE=mysql
echo "Khoi dong backend voi profile: $SPRING_PROFILES_ACTIVE, DB_URL=$DB_URL"
./mvnw spring-boot:run
