#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Configuration
BASE_BRANCH="mvp_demo" #hotspot_label"  #"main"
INTEGRATION_BRANCH="integration-test-$(date +%s)"

# Ensure at least one PR number is provided
if [ "$#" -eq 0 ]; then
    echo "Usage: $0 <PR_NUMBER_1> [<PR_NUMBER_2> ...]"
    echo "Example: $0 124 135 142"
    exit 1
fi

PR_NUMBERS=("$@")

echo "Starting interactive integration build process..."

# 1. Update the base branch
echo "Fetching latest changes for $BASE_BRANCH..."
git checkout $BASE_BRANCH
git pull origin $BASE_BRANCH

# 2. Create the temporary integration branch
echo "Creating temporary integration branch: $INTEGRATION_BRANCH..."
git checkout -b $INTEGRATION_BRANCH

# 3 & 4. Fetch and merge each PR
for PR in "${PR_NUMBERS[@]}"; do
    echo "----------------------------------------"
    echo "Fetching PR #$PR..."
    git fetch upstream pull/$PR/head:pr-$PR
    
    echo "Merging PR #$PR into integration branch..."
    
    # Disable exit-on-error temporarily to catch the conflict
    set +e
    git merge --no-ff pr-$PR -m "Merge PR #$PR for integration testing"
    MERGE_STATUS=$?
    set -e
    
    if [ $MERGE_STATUS -ne 0 ]; then
        echo "----------------------------------------"
        echo "⚠️  CONFLICT DETECTED: PR #$PR has merge conflicts."
        echo "The script is currently paused."
        echo ""
        echo "Action Required:"
        echo "  1. Open a new terminal window or your code editor."
        echo "  2. Resolve the conflicts manually."
        echo "  3. Stage the resolved files (git add <file>)."
        echo "  4. Finish the merge (git commit)."
        echo "----------------------------------------"
        
        while true; do
            read -p "Press [Enter] once you have committed the merge, or type 'abort' to cancel: " USER_INPUT
            
            # Allow the user to bail out if the conflict is too messy
            if [[ "${USER_INPUT,,}" == "abort" ]]; then
                echo "Aborting the process. Cleaning up..."
                git merge --abort || true
                git checkout $BASE_BRANCH
                git branch -D $INTEGRATION_BRANCH
                git branch -D pr-$PR
                exit 1
            fi
            
            # Safety check: Verify the working directory is actually clean
            if [ -z "$(git status --porcelain)" ]; then
                echo "✅ Conflicts resolved and committed! Continuing to the next PR..."
                break
            else
                echo "❌ The working directory is still dirty. Did you forget to commit the changes?"
                echo "   Make sure to run 'git commit' after fixing conflicts."
            fi
        done
    else
        echo "✅ PR #$PR merged successfully."
    fi
    
    # Clean up the local PR branch reference
    git branch -D pr-$PR
done

echo "----------------------------------------"
echo "All PRs merged successfully."

# Detect which repository we are currently in
REPO_URL=$(git config --get remote.origin.url)

if [[ "$REPO_URL" == *"autotune"* ]]; then
    echo "🏗️ Detected Autotune repo. Running Autotune build script..."
    ./build.sh
else
    echo "Running Optimizer build script..."
    ./build_and_push.sh
fi

#echo "Cleaning up git branches..."
#git checkout $BASE_BRANCH
#git branch -D $INTEGRATION_BRANCH

echo "🎉 Done! Your integrated image is ready for testing."
