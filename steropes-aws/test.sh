#!/bin/bash
DATA='{"branchNumber":"123", "maxBalance":1000.00}'
IADURL="http://IADSta-VpcAl-Ll0I9qweQPdp-1927427958.us-east-1.elb.amazonaws.com/account/open"
CMHURL="http://CMHSta-VpcAl-p5tRMwo8YBXL-139366903.us-east-2.elb.amazonaws.com/account/open"

curl -X POST -H "Content-Type: application/json" -d "$DATA" -v -w '%{time_total}' $IADURL > 1.txt 2>&1 &
curl -X POST -H "Content-Type: application/json" -d "$DATA" -v -w '%{time_total}' $CMHURL > 2.txt 2>&1 &
curl -X POST -H "Content-Type: application/json" -d "$DATA" -v -w '%{time_total}' $IADURL > 3.txt 2>&1 &
curl -X POST -H "Content-Type: application/json" -d "$DATA" -v -w '%{time_total}' $CMHURL > 4.txt 2>&1 &

wait
