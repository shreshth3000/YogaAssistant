# Contributing to YogaAssistant

## Development Workflow

### Branching Strategy

Use a feature branch workflow with the following structure:

- `main` - Production-ready code
- `develop` - Integration branch for next release
- `feature/*` - Feature branches for new functionality
- `bugfix/*` - Bug fix branches
- `research/*` - Experimental or research work
- `docs/*` - Documentation updates

### Creating a Feature Branch

1. Branch from `develop`:

   ```
   git checkout develop
   git pull origin develop
   git checkout -b feature/your-feature-name
   ```

2. Make commits with clear messages:

   ```
   git commit -m "Add pose detection feature"
   ```

3. Push to remote:

   ```
   git push origin feature/your-feature-name
   ```

4. Create a Pull Request to `develop`

### Pull Request Process

1. Ensure all tests pass locally
2. Provide clear description of changes
3. Link related issues
4. Request review from maintainers
5. Address review feedback
6. Merge only after approval

## Features Under Development

### High Priority

#### Explainable AI for Pose Corrections

- Branch: `feature/explainable-ai`
- Add visual explanations for why poses are marked incorrect
- Highlight specific joints causing issues
- Suggest corrective actions

#### Performance Optimization

- Branch: `feature/performance`
- Reduce model inference time
- Implement frame skipping strategies
- Add memory profiling

#### Multi-Pose Sequences

- Branch: `feature/pose-sequences`
- Support yoga flows and sequences
- Track transition between poses
- Provide sequence recommendations

### Medium Priority

#### Advanced Filtering

- Branch: `feature/advanced-filters`
- Add more user profile parameters
- Implement time-based recommendations
- Add skill progression tracking

#### Analytics Dashboard

- Branch: `feature/analytics`
- Track user progress
- Generate performance reports
- Visualize improvement metrics

#### Accessibility Features

- Branch: `feature/accessibility`
- Voice guidance support
- Screen reader compatibility
- High contrast mode

### Future Research

#### Machine Learning Improvements

- Branch: `research/ml-improvements`
- Custom pose detection models
- Transfer learning experiments
- Real-time inference optimization

## Setup for Development

### Android Development

```
git clone https://github.com/shreshth3000/YogaAssistant.git
cd YogaAssistant
# Open android/ in Android Studio
# Build and run
```

### Backend Development

```
cd YogaAssistant/backend/recommender
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
python -m uvicorn recommendation_backend:app --reload --port 8000
```

## Code Standards

### Android (Kotlin)

- Use Kotlin conventions from Jetbrains
- Format code with kotlinter or Android Studio formatter
- Maximum line length: 120 characters
- Use meaningful variable names
- Add comments for complex logic

### Python

- Follow PEP 8 style guide
- Use type hints for function parameters and returns
- Format with black or autopep8
- Maximum line length: 100 characters
- Use docstrings for all functions

### General

- No commented-out code in commits
- Keep commits atomic and focused
- Write descriptive commit messages
- Test locally before pushing

## Testing

### Running Tests

Android:

```
./gradlew test
./gradlew connectedAndroidTest
```

Backend:

```
pytest tests/
```

## Documentation

- Update README.md for user-facing changes
- Add docstrings to all new functions
- Document API changes
- Update this file for new branching requirements

## Issue Labels

- `bug` - Something is not working
- `enhancement` - New feature or improvement
- `research` - Experimental work
- `documentation` - Documentation updates
- `help wanted` - Looking for contributor assistance
- `blocked` - Blocked by another issue

## Review Criteria

Before merging:

- Code compiles without errors
- All tests pass
- No significant performance degradation
- Follows project code standards
- Has appropriate documentation
- Addresses all review comments

## Reporting Issues

Include:

- Clear description of issue
- Steps to reproduce
- Expected vs actual behavior
- Device/OS information (for Android issues)
- Python version and environment (for backend issues)
- Screenshots or logs if applicable

## Communication

- GitHub Issues for feature requests and bugs
- Pull Request comments for code discussions
- Keep discussions focused and respectful

## License

All contributions are under the same license as the project. By contributing, you agree to release your code under the project license.
