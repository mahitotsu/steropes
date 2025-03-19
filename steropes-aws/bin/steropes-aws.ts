#!/usr/bin/env node

import { App } from "aws-cdk-lib";
import { SteropesAwsStack } from "../lib/steropes-aws-stack";

const app = new App();

new SteropesAwsStack(app, 'IADStack', {
  env: { account: process.env.CDK_DEFUALT_ACCOUNT, region: 'us-east-1' },
  dsqlEndpoint: '4iabtwnq2j55iez4j4bkykghgm.dsql.us-east-1.on.aws',
});
new SteropesAwsStack(app, 'CMHStack', {
  env: { account: process.env.CDK_DEFUALT_ACCOUNT, region: 'us-east-2' },
  dsqlEndpoint: 's4abtwnq2jebk7aj6vhlsb2coi.dsql.us-east-2.on.aws',
});