# Snow Project Instructions

This project is an Android icon pack.

## Core Mission: PR Cleanup
Our current priority is processing all outstanding Pull Requests (PRs).

### PR Processing Workflow
1. **Research**: Examine the proposed changes in the PR.
   - Note: Logic changes in PRs are often already integrated; prioritize a positive acknowledgement of the contributor even if changes are redundant.
2. **Evaluation**: Decide if the changes align with the project's quality standards and aesthetic. Focus primarily on contributed icons.
3. **Merge Strategy**: 
   - Prioritize strategies that maintain contributor credit (e.g., `git merge` or `git cherry-pick` preserving authorship).
   - If manual adjustments are needed, ensure the contributor is credited in the commit message or `generated/contributors.xml`.
4. **Icon Management**:
   - All contributed icons MUST be in `newicons/`.
   - If a contributor modifies `icons/white/` or `other/`, copy these changes to `newicons/`.
   - The `preparehelper/` scripts will handle further processing.
   - Only two XML files (newdrawables and appfilter) in `newicons/` are permitted to be changed.

## Standards & Conventions
- **Icon Style**: Pure white glyphs (based on `icons/white/` directory).
- **Tooling**: 
    - **DEPRECATED**: The `scripts/` directory is legacy and should NOT be used.
    - **ACTIVE**: Current tools and logic reside in the `preparehelper/` directory.
- **Credit**: Always maintain `generated/contributors.xml` if applicable.
