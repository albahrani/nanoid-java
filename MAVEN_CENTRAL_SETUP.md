# Maven Central Publishing Setup Guide

This project is configured for publishing to Maven Central (not GitHub Packages). Maven Central provides easier consumption for end users as it requires no authentication for downloading public packages.

## Prerequisites

Before you can publish to Maven Central, you need to complete several setup steps:

### 1. Create Sonatype JIRA Account

1. Go to [Sonatype JIRA](https://issues.sonatype.org/secure/Signup!default.jspa)
2. Create an account
3. Create a new project ticket to claim your groupId `de.albahrani.nanoid`
   - Issue Type: "New Project"
   - Group Id: `de.albahrani.nanoid`
   - Project URL: `https://github.com/albahrani/nanoid-java`
   - SCM URL: `https://github.com/albahrani/nanoid-java.git`

### 2. Domain Verification

Since you're using `de.albahrani.nanoid`, you need to prove you control the domain `albahrani.de`:

**Option A: Add DNS TXT record**
- Add TXT record: `OSSRH-[ticket-number]` to `albahrani.de`

**Option B: Create redirect**
- Create redirect from `http://albahrani.de` to your GitHub repository

**Option C: Use GitHub-based groupId (easier)**
- Change groupId to `io.github.albahrani` (no domain verification needed)

### 3. Generate GPG Key

```bash
# Generate a new GPG key
gpg --gen-key

# List your keys
gpg --list-secret-keys --keyid-format LONG

# Export your public key (replace KEY_ID with your actual key ID)
gpg --armor --export KEY_ID

# Export your private key for GitHub secrets
gpg --armor --export-secret-keys KEY_ID
```

### 4. Upload GPG Key to Keyservers

```bash
# Upload to multiple keyservers
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
```

### 5. Configure GitHub Secrets

In your GitHub repository settings → Secrets and variables → Actions, add:

- `OSSRH_USERNAME`: Your Sonatype JIRA username
- `OSSRH_TOKEN`: Your Sonatype JIRA password
- `GPG_PRIVATE_KEY`: Your GPG private key (from step 3)
- `GPG_PASSPHRASE`: Your GPG key passphrase

## Alternative: Use GitHub-based GroupId

If domain verification is complex, change the groupId to avoid it:

```xml
<groupId>io.github.albahrani</groupId>
```

This requires no domain verification, just GitHub account ownership proof.

## Testing the Setup

### Local Test Build
```bash
# Test that everything builds correctly
mvn clean verify -P release

# This should generate:
# - target/nanoid-java-1.1.0.jar
# - target/nanoid-java-1.1.0-sources.jar  
# - target/nanoid-java-1.1.0-javadoc.jar
# - target/nanoid-java-1.1.0.jar.asc (and other .asc files)
```

### Dry Run Deploy
```bash
# Test deploy without actually publishing
mvn clean deploy -P release -DaltDeploymentRepository=local::default::file:./target/staging-deploy
```

## Publishing Process

Once setup is complete:

1. **Update version** in `pom.xml` for release
2. **Create GitHub release** with tag (e.g., `v1.1.0`)
3. **GitHub Actions automatically**:
   - Builds the project
   - Generates sources and javadoc JARs
   - Signs all artifacts with GPG
   - Uploads to Sonatype staging repository
   - Auto-releases to Maven Central

## After Publishing

It takes 2-4 hours for artifacts to appear in Maven Central search.

Users can then simply add:

```xml
<dependency>
    <groupId>de.albahrani.nanoid</groupId>
    <artifactId>nanoid-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

**No authentication required!** 🎉

## Troubleshooting

### Common Issues:

1. **GPG signing fails**: Ensure GPG_PRIVATE_KEY is complete (including headers/footers)
2. **Domain verification pending**: Check your Sonatype JIRA ticket status
3. **Artifact validation fails**: Ensure all required metadata is present in pom.xml

### Support:
- [Sonatype Documentation](https://central.sonatype.org/publish/publish-guide/)
- [GitHub Discussions](https://github.com/albahrani/nanoid-java/discussions)

## Next Steps

1. Complete Sonatype JIRA ticket process
2. Set up GPG key and GitHub secrets  
3. Test with a snapshot release first
4. Create your first official release!
