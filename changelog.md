# Changelog

## 1.1 (2015-12-4)

#### New Features

- Dynamic Scaling of Workers
- Support for DASH manifest based Variants
- Support for Smooth Streaming manifest based Variants
- Location Management
- FTP/SFTP repositories

#### Improvements

- Index performance improvements

#### Bug Fixes

- Omakase Worker leaks HTTP Connections

## 1.0.2 (2015-11-12)

#### Improvements

- Upgraded to Swagger 2.0
- Upgraded to Modeshape 4.4.0.Final
- Enabled Infinispan Cache Eviction

#### Bug Fixes

- Unable to connect to S3/Glacier if the AWS secret key contains a forward slash
- Invalid repository id in job configuration payload results in incorrect error message.
- Unsupported REST API version number in header results in incorrect message

## 1.0.1 (2015-10-15)

#### Improvements

- Use a unique ID to identify a repository instead of it's name. 
- Reject job submissions for a given variant if a destructive (delete) job is currently executing against the variant.
- Compress the task payloads prior to writing them to a message queue.
- Execute the first stage of the pipeline asynchronously instead of as part of the submit job request.

#### Bug Fixes

- Creating export/replicate jobs for HLS variants is very slow.
- A variant can be deleted from a repository while it is being replicated.
- Task payloads can exceed the SQS message size limit.
- Unable to store Task configurations and outputs that exceed 4KB.

## 1.0.0 (2015-10-01)

No changes.

## 0.9.0 (2015-09-30)

#### New Features

- Added JCR Indexes. This improves the performance of many read operations.
- Workers can now be pre-registered prior to the worker instance starting.

#### Improvements

- Support throttling the number of task status updates that can be processed per consumer, over a given period.
- Replaced PicketLink with Wildfly roles/users. Users can now be managed via the Wildfly user scripts.
- Several enhancements to Job Configuration validation.
- Replaced quickstart.sh with a Docker Compose script. This can be used to launch a full Omakase instance on a single node.
- Integration tests no longer need to restart the container between each deployment.
- Moved load testing module to a separate GIT repository.
- Removed omakase-config module. All Omakase config is now part of the omakase-dist-wildfly module.
- Upgraded to Wildfly Camel 2.3.0.

#### Bug Fixes

- Nodes in an Omakase cluster do not always see changes made by another node.
- Infinispan JDBC cache store throws deadlock exceptions when purging cache.
- Performance Issue: POST /jobs quickly degrades load
- Variant File HLS Stream name contains null if the manifest does not have a resolution for the stream.

## 0.8.0 (2015-07-16)

#### New Features

- Export variants to a S3 bucket. 
- Ingest and Replicate Http Live Streaming (HLS) variants into File and S3 backed repositories.
- Export HLS variants to File, S3, FTP, and SFTP locations.
- Automatic retrying of failed tasks, the number of retires can be configured via `omakase.max.task.retries`.
- Retrieve messages associated with a job via the REST API.
- Retrieve messages associated with a worker via the REST API.

#### Improvements

- Optimize worker based deletion by supporting multiple delete locations in a single delete task.
- Optimize file backed repositories by segmenting files into multiple directories.

#### Bug Fixes

- Fix stuck export and replication jobs when the source repository does not contain the variant's files
- Fix /assets/<asset id>/variants/<variant id>/files self link
- Fix hash generation error
- Fix PUT /repositories/<name>/configuration when specifying no payload or an empty JSON payload
- Fix exporting/replication/deleting from a AWS Glacier back repository
- Fix reduce technical debt accumulated in the last few releases
