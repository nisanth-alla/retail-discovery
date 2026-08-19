# Contributing

Thanks for considering a contribution to Visual Retail Discovery. This is a personal engineering project continued independently from an event prototype, and it is maintained with a small set of conventions.

## Getting started

1. Read the [README](README.md) for the architecture and tech stack.
2. Open an issue or discussion before large changes so the approach is agreed in advance.
3. Keep changes focused. Prefer a small, reviewable pull request over a large one.

## Development rules

- **Backend**: Java 17 with Spring Boot and Maven. Follow the existing package layout under `src/main/java`.
- **Frontend**: React + Vite under `frontend/`. Keep the responsive desktop/mobile behavior and accessibility.
- **Python**: The `python_module/` holds inference-related utilities. Keep it isolated from the Java service boundary.
- **Model files**: Do not commit model weights (`.onnx`, `.pt`, and other large inference artifacts). They are gitignored. Document how to obtain or build them instead.
- **Secrets**: Never commit `.env`, API keys, or credentials. Use the deployment platform's secret store.
- **Tests**: Add or update tests for behavior changes and run the project's configured check commands.

## Commit and publishing

Leave commits and publishing to the repository owner. Ensure your local changes pass the relevant checks before opening a pull request.

## License

By contributing, you agree that your contributions are licensed under the same [MIT License](LICENSE) that covers this project.
