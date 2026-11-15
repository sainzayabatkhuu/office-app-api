#!/usr/bin/env bash
# End-to-end check of a running platform through the gateway:
# login -> profile -> upload sample report -> SYNC PDF -> ASYNC XLSX -> notification.
# Usage: ./scripts/smoke-test.sh [gateway-url]   (requires curl + jq)
set -euo pipefail
GW=${1:-http://localhost:8080}
cd "$(dirname "$0")/.."
CODE="SMOKE_$(date +%s)"

step() { printf '\n== %s\n' "$*"; }
fail() { echo "FAILED: $*" >&2; exit 1; }

step "Unauthenticated request is rejected by the gateway"
status=$(curl -s -o /dev/null -w '%{http_code}' "$GW/api/v1/reports")
[ "$status" = 401 ] || fail "expected 401, got $status"
echo "401 OK"

step "Login as admin"
TOKEN=$(curl -sf "$GW/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}' | jq -r .accessToken)
[ -n "$TOKEN" ] && [ "$TOKEN" != null ] || fail "login"
AUTH="Authorization: Bearer $TOKEN"
curl -sf "$GW/api/v1/auth/me" -H "$AUTH" | jq -c '{username, displayName, branchCode, roles}'

step "Upload sample report $CODE"
REPORT_JSON=$(jq -nc --arg code "$CODE" '{code:$code, metadata:{title:"Тайлангийн ангилал", allowedFormats:["PDF","XLSX","CSV"], defaultFormat:"PDF", executionMode:"SYNC", active:true}}')
REPORT_ID=$(curl -sf "$GW/api/v1/reports" -H "$AUTH" \
  -F "report=$REPORT_JSON;type=application/json" \
  -F "file=@samples/reports/jrxml/report_categories.jrxml;type=application/xml" | jq -r .report.id)
echo "report id = $REPORT_ID"

step "Run SYNC (PDF) and download"
EXEC=$(curl -sf "$GW/api/v1/reports/$REPORT_ID/executions" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"format":"PDF","parameters":{"minSortOrder":"10"}}')
echo "$EXEC" | jq -c '{status, pageCount, outputFileName, durationMs}'
[ "$(echo "$EXEC" | jq -r .status)" = SUCCESS ] || fail "sync execution"
curl -sf "$GW$(echo "$EXEC" | jq -r .downloadUrl)" -H "$AUTH" -o /tmp/office-smoke.pdf
head -c 4 /tmp/office-smoke.pdf | grep -q '%PDF' || fail "download is not a PDF"
echo "PDF downloaded: $(wc -c </tmp/office-smoke.pdf) bytes"

step "Switch to ASYNC and run (XLSX)"
curl -sf -X PUT "$GW/api/v1/reports/$REPORT_ID" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"title":"Тайлангийн ангилал","allowedFormats":["PDF","XLSX"],"defaultFormat":"XLSX","executionMode":"ASYNC","active":true}' >/dev/null
UNREAD_BEFORE=$(curl -sf "$GW/api/v1/notifications/unread-count" -H "$AUTH" | jq .count)
EXEC_ID=$(curl -sf "$GW/api/v1/reports/$REPORT_ID/executions" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"parameters":{}}' | jq -r .id)
for _ in $(seq 1 30); do
  S=$(curl -sf "$GW/api/v1/report-executions/$EXEC_ID" -H "$AUTH" | jq -r .status)
  [ "$S" = SUCCESS ] || [ "$S" = FAILED ] && break
  sleep 1
done
echo "async status = $S"
[ "$S" = SUCCESS ] || fail "async execution"

step "Notification arrived"
for _ in $(seq 1 10); do
  UNREAD=$(curl -sf "$GW/api/v1/notifications/unread-count" -H "$AUTH" | jq .count)
  [ "$UNREAD" -gt "$UNREAD_BEFORE" ] && break
  sleep 1
done
[ "$UNREAD" -gt "$UNREAD_BEFORE" ] || fail "no notification"
curl -sf "$GW/api/v1/notifications?size=1" -H "$AUTH" | jq -c '.content[0] | {level, title, message}'

step "Aggregated API docs"
for s in identity-service report-service notification-service; do
  curl -sf "$GW/$s/v3/api-docs" | jq -r '.info.title'
done

echo; echo "ALL CHECKS PASSED"
