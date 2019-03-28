import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

    final static Regions REGION = Regions.US_EAST_1;
    final static String TABLE_NAME = "csye6225";
    final static String DYNAMO_KEY = "id";
    final static String FROM_EMAIL = System.getenv("FROM_EMAIL");

    public Object handleRequest(SNSEvent request, Context context) {
        context.getLogger().log("Invocation started: " + getTimeStamp());
        context.getLogger().log("1: " + (request == null));
        context.getLogger().log("2: " + (request.getRecords().size()));
        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

        AmazonDynamoDB AmazonDynamoDBclient = AmazonDynamoDBClientBuilder.standard().withRegion(REGION).build();
        context.getLogger().log("Reayd: Init DynamoDB Client");
        DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBclient);
        context.getLogger().log("Reayd: Get DynamoDB Instance");
        Table table = dynamoDB.getTable(TABLE_NAME);
        context.getLogger().log("Reayd: Get DynamoDB Table");
        String toEmail = request.getRecords().get(0).getSNS().getMessage().split(",")[0];
        context.getLogger().log("Reayd: Read Email from input");
        Item item = table.getItem(DYNAMO_KEY, toEmail);
        String token = UUID.randomUUID().toString().replaceAll("-","");
//        String token = "123";
        context.getLogger().log("Reayd: Get Random String");
        context.getLogger().log("3: " + token);
        if (item == null) {
            item = new Item().withPrimaryKey(DYNAMO_KEY, toEmail).with("token", token)
                    .with("ttl", ((System.currentTimeMillis() / 1000 + 1200)));
            PutItemOutcome outcome = table.putItem(item);
            context.getLogger().log("Put successful: " + outcome.toString());
        } else {
            context.getLogger().log("Record Already Present");
            return null;
        }
        String subject = "Reset Password Request";
        StringBuilder emailBodyBuilder = new StringBuilder();
        emailBodyBuilder.append("<p>You are receiving this mail because to chosegi to reset your password.</p>\n");
        emailBodyBuilder.append("<p>Please click the link below to Reset your password:</p>\n");
        emailBodyBuilder.append("<p> Link: <a href='http://example.com/reset?email=" + toEmail + "&token=" + token+"'>" + "http://example.com/reset?email=" + toEmail + "&token=" + token + "</a></p>\n");
        emailBodyBuilder.append("<p>This link will only be valid for 20 minuits starting: " + getTimeStamp() + "</p>");
        context.getLogger().log("4: " + emailBodyBuilder.toString());
        try {
            AmazonSimpleEmailService amazonSimpleEmailService = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
            Destination destination = new Destination().withToAddresses(toEmail);
            Content bodyContent = new Content().withCharset("ISO-8859-1").withData(emailBodyBuilder.toString());
            Content subContent = new Content().withCharset("ISO-8859-1").withData(subject);
            Message message = new Message().withBody(new Body().withHtml(bodyContent)).withSubject(subContent);
            SendEmailRequest emailRequest = new SendEmailRequest().withDestination(destination).withMessage(message).withSource(FROM_EMAIL);
            SendEmailResult result = amazonSimpleEmailService.sendEmail(emailRequest);
            context.getLogger().log("Request result: " + result.toString());
        } catch (Exception e) {
            context.getLogger().log("Exception : " + e.getMessage());
        }
        context.getLogger().log("Lambda Completed: " + getTimeStamp());
        return null;
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    }

}