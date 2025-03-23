#!/usr/bin/env node

import { App } from "aws-cdk-lib";
import { SteropesAwsStack } from "../lib/steropes-aws-stack";

const app = new App();

new SteropesAwsStack(app, 'IADStack', {
  env: { account: process.env.CDK_DEFUALT_ACCOUNT, region: 'us-east-1' },
  dsqlClusterId: '4iabtwnq2j55iez4j4bkykghgm',
  lockTable: 'lock_table',
});
new SteropesAwsStack(app, 'CMHStack', {
  env: { account: process.env.CDK_DEFUALT_ACCOUNT, region: 'us-east-2' },
  dsqlClusterId: 's4abtwnq2jebk7aj6vhlsb2coi',
  lockTable: 'lock_table',
});