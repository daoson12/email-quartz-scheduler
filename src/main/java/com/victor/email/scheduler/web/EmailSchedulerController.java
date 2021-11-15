package com.victor.email.scheduler.web;

import com.victor.email.scheduler.payload.EmailRequest;
import com.victor.email.scheduler.payload.EmailResponse;
import com.victor.email.scheduler.quartz.job.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@Slf4j
@RestController
public class EmailSchedulerController {

 @Autowired
 private Scheduler scheduler;

 @PostMapping ("/schedule/email")
public ResponseEntity<EmailResponse> scheduleEmail(@Valid @RequestBody EmailRequest emailRequest){
     try{

         ZonedDateTime dateTime=ZonedDateTime.of(emailRequest.getDateTime(),
                 emailRequest.getTimeZone());
         if (dateTime.isBefore(ZonedDateTime.now())) {
             EmailResponse emailResponse= new EmailResponse(false, "dateTime must be after current time.");
             return ResponseEntity.badRequest()
                     .body(emailResponse);
         }

         JobDetail jobDetail=buildJobDetail(emailRequest);
         Trigger trigger = buildTrigger(jobDetail,dateTime);
         scheduler.scheduleJob(jobDetail,trigger);

         EmailResponse emailResponse= new EmailResponse(true, jobDetail
         .getKey().getName(), jobDetail.getKey().getGroup(),
                 "Email scheduled success");
         return ResponseEntity.ok(emailResponse);

     }catch (SchedulerException e){
         log.error("Error while scheduling email: ", e);
         EmailResponse emailResponse = new EmailResponse(false,
                 "Error while scheduling email. Please try again later");
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                 .body(emailResponse);
     }


 }
@GetMapping("/get")
public ResponseEntity<String> getApiTest(){
    return ResponseEntity.ok("Get API Test - Pass");
}

    private JobDetail buildJobDetail(EmailRequest emailRequest){
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", emailRequest.getEmail());
        jobDataMap.put("subject", emailRequest.getSubject());
        jobDataMap.put("body", emailRequest.getBody());

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(), "email-jobs")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-trigger")
                .withDescription("Send Email Trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
                .build();
    }
}
