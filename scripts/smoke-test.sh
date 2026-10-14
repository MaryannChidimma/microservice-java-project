#!/usr/bin/env bash
# Smoke test: place one order through the api-gateway, then delete the test data.
#
# Needs the gateway reachable, e.g. in another terminal:
#   kubectl port-forward svc/api-gateway 8000:80
# Then:
#   ./scripts/smoke-test.sh
#
# GATEWAY_URL overrides the address (default http://localhost:8000).
# Prints PASS, or FAIL with the first step that failed; exits 0 or 1.
# The order it places stays in the database: there is no endpoint to delete orders.
set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8000}"
NAME="smoke-$(date +%s)"
USER_ID=""
PRODUCT_ID=""
FAILED=""

TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

# request METHOD PATH [JSON_BODY]: sends one request, sets STATUS and BODY.
# A body is passed as one argument, never inside "$(...)": macOS bash 3.2
# brace-expands {..,..} there and sends broken JSON.
request() {
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$1" "$GATEWAY_URL$2")
  if [ -n "${3:-}" ]; then
    args+=(-H 'Content-Type: application/json' --data "$3")
  fi
  STATUS=$(curl "${args[@]}") || STATUS=000
  BODY=$(cat "$TMP")
}

# check STEP EXPECTED_STATUS: prints the result and remembers the first failure
check() {
  if [ "$STATUS" = "$2" ]; then
    echo "ok    $1 -> $STATUS"
    return 0
  fi
  echo "FAIL  $1 -> $STATUS (expected $2) $BODY"
  [ -z "$FAILED" ] && FAILED="$1"
  return 1
}

echo "Smoke test against $GATEWAY_URL (test data: $NAME)"

# Each step runs only if all earlier steps passed; cleanup always runs.
steps() {
  # 1. Create a user (user-service)
  request POST /users '{"name":"'"$NAME"'","email":"'"$NAME"'@example.test"}'
  check "create user" 201 || return
  USER_ID=$(printf '%s' "$BODY" | sed -nE 's/.*"id":([0-9]+).*/\1/p')
  [ -n "$USER_ID" ] || { echo "FAIL  create user: no id in $BODY"; FAILED="create user"; return; }

  # 2. Create a product (product-service, MongoDB id is a string)
  request POST /products '{"name":"'"$NAME"'","description":"smoke test item","price":1.00,"stockQuantity":10,"category":"smoke-test"}'
  check "create product" 201 || return
  PRODUCT_ID=$(printf '%s' "$BODY" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
  [ -n "$PRODUCT_ID" ] || { echo "FAIL  create product: no id in $BODY"; FAILED="create product"; return; }

  # 3. Add the product to the user's cart (order-service calls product-service)
  request POST "/carts/$USER_ID/items" '{"productId":"'"$PRODUCT_ID"'","quantity":1}'
  check "add to cart" 200 || return

  # 4. Place the order from the cart (order-service calls user-service)
  request POST "/orders/$USER_ID"
  check "place order" 201 || return
}

# 5. Delete the user and product this run created, even after a failure
cleanup() {
  if [ -n "$USER_ID" ]; then
    request DELETE "/users/$USER_ID"
    check "delete user $USER_ID" 204
  fi
  if [ -n "$PRODUCT_ID" ]; then
    request DELETE "/products/$PRODUCT_ID"
    check "delete product $PRODUCT_ID" 204
  fi
}

steps
cleanup

# 6. Result
if [ -z "$FAILED" ]; then
  echo "PASS"
  exit 0
fi
echo "FAIL: $FAILED"
exit 1
