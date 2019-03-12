package com.github.cafdataprocessing.services.staging.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import com.github.cafdataprocessing.services.staging.exceptions.PathException;
import com.github.cafdataprocessing.services.staging.utils.DiskInfo;
import com.github.cafdataprocessing.services.staging.utils.DiskSpaceChecker;

@Component
public class StagingHealth extends AbstractHealthIndicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingHealth.class);

    private final String path;
    // In MB
    private final Long threshold;

    public StagingHealth(@Value("${staging.basePath}") final String path,
                         @Value("${staging.diskSizeThresholdMb}") final Long threshold) {
        super("Staging disk space health check failed");
        this.path = path;
        this.threshold = threshold;
    }

    @Override
    protected void doHealthCheck(final Health.Builder builder) throws Exception {
        try
        {
            final DiskInfo diskInfo = DiskSpaceChecker.getDiskInformation(this.path);
            final long diskFreeInBytes = diskInfo.getUsableSpace();
            final long thresholdInBytes = this.threshold * 1024 * 1024;
            if (diskFreeInBytes >= thresholdInBytes) {
                builder.up();
            }
            else 
            {
                LOGGER.warn(String.format(
                        "Free disk space for staging batches is below threshold. Available: %d bytes (threshold: %s)",
                        diskFreeInBytes, this.threshold));
                builder.down();
            }
            builder.withDetail(path + ": total space", diskInfo.getTotalSpace())
                   .withDetail(path + ": free space", diskFreeInBytes)
                   .withDetail("threshold:", this.threshold);
        }
        catch(final PathException e)
        {
            LOGGER.warn(String.format("Staging path not found: %s", this.path));
            //TODO: Should the staging base folder be created when the service starts up?
            builder.up();
            builder.withDetail(path + ": total space", "unknown")
               .withDetail(path + ": free space", "unknown")
               .withDetail("threshold (Mb):", this.threshold);
        }
    }
  }