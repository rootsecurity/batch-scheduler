package com.asofdate.batch.core;

import com.asofdate.batch.entity.TaskArgumentEntity;
import com.asofdate.batch.service.ArgumentService;
import com.asofdate.batch.service.TaskStatusService;
import com.asofdate.utils.JoinCode;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.List;
import java.util.UUID;

/**
 * Created by hzwy23 on 2017/5/21.
 */
public class QuartzJobLauncher extends QuartzJobBean {

    private final Logger logger = LoggerFactory.getLogger(QuartzJobLauncher.class);

    private JobLauncher jobLauncher;
    private JobRegistry jobRegistry;
    //    private JobExplorer jobExplorer;
//    private JobOperator jobOperator;
    private TaskStatusService taskStatusService;
    private ArgumentService argumentService;
    private String jobName;

    public ArgumentService getArgumentService() {
        return argumentService;
    }

    public void setArgumentService(ArgumentService argumentService) {
        this.argumentService = argumentService;
    }

    public TaskStatusService getTaskStatusService() {
        return taskStatusService;
    }

    public void setTaskStatusService(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

//    public JobOperator getJobOperator() {
//        return jobOperator;
//    }
//
//    public void setJobOperator(JobOperator jobOperator) {
//        this.jobOperator = jobOperator;
//    }
//
//    public JobExplorer getJobExplorer() {
//        return jobExplorer;
//    }
//
//    public void setJobExplorer(JobExplorer jobExplorer) {
//        this.jobExplorer = jobExplorer;
//    }

    public JobRegistry getJobRegistry() {
        return jobRegistry;
    }

    public void setJobRegistry(JobRegistry jobRegistry) {
        this.jobRegistry = jobRegistry;
    }

    public JobLauncher getJobLauncher() {
        return jobLauncher;
    }

    public void setJobLauncher(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;
    }

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        try {

            Job job = jobRegistry.getJob(jobName);

            JobExecution jobExecution = jobLauncher.run(job, getJobParameters());

            if (ExitStatus.COMPLETED.getExitCode().equals(jobExecution.getExitStatus().getExitCode())) {
                taskStatusService.setTaskCompleted(jobName);
                jobRegistry.unregister(jobName);
            } else {
                taskStatusService.setTaskError(jobName);
            }

        } catch (NoSuchJobException e) {
            e.printStackTrace();
        } catch (JobInstanceAlreadyCompleteException e) {
            e.printStackTrace();
        } catch (JobExecutionAlreadyRunningException e) {
            e.printStackTrace();
        } catch (JobParametersInvalidException e) {
            e.printStackTrace();
        } catch (JobRestartException e) {
            e.printStackTrace();
        }
    }

    public JobParameters getJobParameters() {
        JobParametersBuilder builder = new JobParametersBuilder();

        String jobId = JoinCode.getTaskCode(jobName);
        List<TaskArgumentEntity> list = argumentService.queryArgument(jobId);
        if (list == null) {
            builder.addString("uuid", UUID.randomUUID().toString());
            return builder.toJobParameters();
        }

        String jobParameters = "";
        for (TaskArgumentEntity m : list) {
            jobParameters += " " + m.getArgValue();
        }
        builder.addString("JobParameters", jobParameters.trim());
        builder.addString("uuid", UUID.randomUUID().toString());

        return builder.toJobParameters();
    }
}
