# Contributing to Snow

Thanks for your interest in contributing to Snow! Whether you're here to request an icon, report a bug, or add new icons, this guide will help you get started.

## Requesting Icons
If you'd like to see a specific app supported, please use the [Issue Tracker](https://github.com/baitmooth/snow/issues/new/choose) and choose the "Requesting New Icons" template. Make sure to provide the app name and a link to the Play Store.

## Reporting Bugs
If you find any issues with existing icons (e.g., broken links, incorrect designs), please use the [Issue Tracker](https://github.com/baitmooth/snow/issues/new/choose) and choose the "Broken Icons" template.

## Contributing Icons
We welcome icon contributions! To ensure a consistent look and feel across the icon pack, please follow these guidelines:

### Icon Style
*   **Design**: Icons should be pure white glyphs with a transparent background.
*   **Format**: Use SVG (Scalable Vector Graphics) format.
*   **Grid**: We recommend using a 48x48 pixel grid with a 2-pixel stroke for consistent line weights.
*   **Consistency**: Check existing icons in the `icons/white` directory to match the established style.
*   **Visuals**: Use **round caps** and **round corners** for all strokes.
*   **Authenticity**: We do not accept icons that are directly image-traced. They must be clean, manually crafted vectors.

### Directory Structure & Icon Management
To keep the project organized and allow for automated processing, we use a specific directory structure:

*   **`newicons/`**: **This is where all new contributions belong.** Whether you are adding a new icon or updating an existing one, please place your SVG files here. Our automation tools (`preparehelper`) monitor this folder to automatically handle categorization, sorting, and the generation of themed variants.
*   **`icons/white/`**: This is the primary library containing finalized white glyphs.

**Why use `newicons/`?**
Using the `newicons/` directory allows our release pipeline to:
1.  **Generate Variants**: Automatically create black and themed versions of your icons.
2.  **Maintain Credit**: Track contributions for the changelog and contributor list.
3.  **Ensure Quality**: Run automated checks against new assets without risk to the main library.
4.  **Clean Integration**: Properly merge and sort entries into the final `appfilter.xml` during the release process.

### SVG Technical Requirements
Before submitting, please open your SVG files in a text editor. To ensure compatibility with Android's Vector Drawable format, avoid the following:

*   **Transform attributes**: These should be baked into the path coordinates (un-group or combine objects).
*   **fill-rule:evenodd**: Use the default non-zero fill rule.
*   **Scientific notation**: Avoid coordinates like `1.2e-4`. Use standard decimal notation.
*   **Empty markers**: Remove any `marker-start=""` or similar empty attributes.

### Submitting Your Changes
1.  **Fork the Repository**: Create a fork of the Snow repository.
2.  **Create a Branch**: Create a new branch for your changes (e.g., `feature/add-new-icons`).
3.  **Add Icons**: Place your new SVG icons in the `newicons/` directory.
4.  **Update `appfilter.xml`**: Add the necessary component info to `newicons/appfilter.xml`.
5.  **Commit and Push**: Commit your changes and push them to your fork.
6.  **Create a Pull Request**: Submit a pull request to the `main` branch of the original Snow repository.

### Attribution
All contributors are credited in the `generated/contributors.xml` file. When you submit a pull request, please ensure your name or GitHub username is included if you'd like to be credited.
