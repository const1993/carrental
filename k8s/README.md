# Kubernetes notes

The manifests include application deployment, service, configuration, an example secret,
a development PostgreSQL StatefulSet, and a rootless BuildKit image-build Job.

The PostgreSQL StatefulSet is **development/demo only**. Production should use managed
PostgreSQL or a properly operated HA database cluster with backup/restore procedures.

The image build Job deliberately assumes source code already exists on a PVC and registry
credentials are provided through a Kubernetes secret. In most production environments,
building/signing images in CI outside the runtime cluster is preferable.
