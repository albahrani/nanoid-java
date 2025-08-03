# Release Guide for nanoid-java

This guide explains how to release a new version of nanoid-java to GitHub Packages.

## Prerequisites

1. **Repository Setup**: Ensure your code is pushed to GitHub repository `albahrani/nanoid-java`
2. **GitHub Actions**: The repository should have the GitHub Actions workflows in `.github/workflows/`
3. **Permissions**: You need admin access to the repository to create releases

## Release Process

### 1. Prepare the Release

1. **Update Version**: Update the version in `pom.xml`:
   ```xml
   <version>1.1.0</version>
   ```

2. **Update README**: Ensure the README has the correct version in installation instructions

3. **Test Locally**: Run tests to ensure everything works:
   ```bash
   mvn clean test
   ```

4. **Commit Changes**:
   ```bash
   git add .
   git commit -m "Prepare release v1.1.0"
   git push origin main
   ```

### 2. Create a GitHub Release

1. **Go to GitHub**: Navigate to your repository: `https://github.com/albahrani/nanoid-java`

2. **Create Release**:
   - Click "Releases" → "Create a new release"
   - Tag version: `v1.1.0` (must start with 'v')
   - Release title: `Release v1.1.0`
   - Description: Add release notes describing changes

3. **Publish Release**: Click "Publish release"

### 3. Automatic Publishing

Once you publish the release, GitHub Actions will automatically:
- Run all tests
- Build the project
- Generate Javadoc and source JARs
- Publish to GitHub Packages

### 4. Verify Publication

1. **Check Actions**: Go to "Actions" tab and verify the "Publish Package" workflow succeeded

2. **Verify Package**: Go to your repository → "Packages" tab to see the published package

## Using the Published Package

### For Maven Users

Add to `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/albahrani/nanoid-java</url>
    </repository>
</repositories>

<dependency>
    <groupId>de.albahrani.nanoid</groupId>
    <artifactId>nanoid-java</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Authentication

Users need to create a GitHub Personal Access Token (PAT) with `read:packages` scope and add it to `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>GITHUB_USERNAME</username>
            <password>GITHUB_PAT</password>
        </server>
    </servers>
</settings>
```

## Next Release

For subsequent releases:
1. Update version in `pom.xml`
2. Update version in README examples
3. Commit and push changes
4. Create new GitHub release with incremented version tag

## Troubleshooting

- **Build Fails**: Check the Actions logs for detailed error messages
- **Permission Denied**: Ensure you have write access to the repository
- **Package Not Found**: Verify the repository name and package coordinates match exactly
