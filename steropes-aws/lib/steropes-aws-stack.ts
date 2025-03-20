import { Stack, StackProps } from "aws-cdk-lib";
import { ContainerImage, FargateTaskDefinition } from "aws-cdk-lib/aws-ecs";
import { Construct } from "constructs";

interface SteropesAwsStackProps extends StackProps {
  dsqlEndpoint: string;
}

export class SteropesAwsStack extends Stack {
  constructor(scope: Construct, id: string, props?: SteropesAwsStackProps) {
    super(scope, id, props);

    const taskDefinition = new FargateTaskDefinition(this, 'ApiTaskDefinition', { cpu: 1024, memoryLimitMiB: 4096, });
    taskDefinition.addContainer('main', {
      image: ContainerImage.fromAsset(`${__dirname}/../../steropes-api`),
      portMappings: [{ containerPort: 8080 }],
      environment: {
        SPRING_DATASOURCE_URL: `jdbc:postgresql://${props!.dsqlEndpoint}/postgres`,
        SPRING_DATASOURCE_USERNAME: 'admin',
        SPRING_DATASOURCE_PASSWORD: 'dummy',
        SPRING_DATASOURCE_PROPERTIES_SSLMODE: 'REQUIRE',
        SPRING_JPA_SHOW_SQL: 'false',
      }
    });
  }
}
