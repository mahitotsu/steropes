import { Duration, Stack, StackProps } from "aws-cdk-lib";
import { SubnetType, Vpc } from "aws-cdk-lib/aws-ec2";
import { Cluster, ContainerImage, FargateService, FargateTaskDefinition, LogDriver } from "aws-cdk-lib/aws-ecs";
import { ApplicationLoadBalancer, ApplicationProtocol, ApplicationTargetGroup, TargetType } from "aws-cdk-lib/aws-elasticloadbalancingv2";
import { Effect, PolicyStatement } from "aws-cdk-lib/aws-iam";
import { Construct } from "constructs";

interface SteropesAwsStackProps extends StackProps {
  dsqlClusterId: string;
  lockTable: string;
}

export class SteropesAwsStack extends Stack {
  constructor(scope: Construct, id: string, props?: SteropesAwsStackProps) {
    super(scope, id, props);

    const taskDefinition = new FargateTaskDefinition(this, 'ApiTaskDefinition', { cpu: 1024, memoryLimitMiB: 4096, });
    taskDefinition.addContainer('main', {
      image: ContainerImage.fromAsset(`${__dirname}/../../steropes-api`),
      portMappings: [{ containerPort: 8080 }],
      environment: {
        SPRING_DATASOURCE_URL: `jdbc:postgresql://${props!.dsqlClusterId}.dsql.${props!.env!.region}.on.aws/postgres`,
        SPRING_DATASOURCE_USERNAME: 'admin',
        SPRING_DATASOURCE_PASSWORD: 'dummy',
        SPRING_DATASOURCE_PROPERTIES_SSLMODE: 'REQUIRE',
        SPRING_JPA_SHOW_SQL: 'false',
      },
      logging: LogDriver.awsLogs({ streamPrefix: 'steropes-api' }),
    });
    taskDefinition.addToTaskRolePolicy(new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'dynamodb:DeleteItem',
        'dynamodb:GetItem',
        'dynamodb:PutItem',
        'dynamodb:Scan',
        'dynamodb:UpdateItem',
        'dynamodb:DescrribeTable',
      ],
      resources: [`arn:aws:dynamodb:${props!.env!.region}:${props!.env!.account}:table/${props!.lockTable}`],
    }));
    taskDefinition.addToTaskRolePolicy(new PolicyStatement({
      effect: Effect.ALLOW,
      actions: [
        'dsql:DbConnect',
        'dsql:DbConnectAdmin'
      ],
      resources: ['*']
    }));

    const vpc = new Vpc(this, 'Vpc', { maxAzs: 2, natGateways: 2, createInternetGateway: true });
    const alb = new ApplicationLoadBalancer(vpc, 'Alb', { vpc, internetFacing: true });

    const cluster = new Cluster(this, 'Cluster', { vpc });
    const service = new FargateService(this, 'ApiService', {
      cluster, taskDefinition, desiredCount: 1,
      assignPublicIp: false, vpcSubnets: { subnetType: SubnetType.PRIVATE_WITH_EGRESS },
      minHealthyPercent: 0, maxHealthyPercent: 200, healthCheckGracePeriod: Duration.seconds(60),
    });

    const targetGroup = new ApplicationTargetGroup(this, 'ApiTargetGroup', {
      vpc, port: 8080, targets: [service], protocol: ApplicationProtocol.HTTP, targetType: TargetType.IP,
      deregistrationDelay: Duration.seconds(0),
      healthCheck: { path: '/actuator/health', interval: Duration.seconds(5), timeout: Duration.seconds(2), },
    });
    alb.addListener('ApiListener', { port: 80, open: true, defaultTargetGroups: [targetGroup] });
  }
}
