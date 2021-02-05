## Preparation of pull secret

Navigate to quay.io and open Account Settings page. Click on the "Generate Encrypted Password"
link and enter your quay.io password. Navigate to "Encrypted Password" and copy the value.
It will be used in the command below.

```
❯ kubectl create secret docker-registry quay-pull-secret --docker-server=quay.io --docker-username=mgoldman --docker-password=<your-pword> --docker-email=mgoldman@domain.com
secret/quay-pull-secret created
```

This pull secret will be used by the `cpaas-bot` Service Account to be able to pull images
from the quay.io registry.
