document.addEventListener('DOMContentLoaded', () => {
  sessionStorage.clear();
  const form = document.querySelector('form');
  const username = document.getElementById('username');
  const password = document.getElementById('password');
  const passwordConfirm = document.getElementById('passwordConfirm');
  const email = document.getElementById('email');
  const phoneNumber = document.getElementById('phoneNumber');

  const validateUsername = (username) => {
    const regex = /^(_?[a-zA-Z0-9]+)+_?$/;
    return regex.test(username) && username.length <= 30;
  };

  const validatePassword = (password) => {
    const regex = /^[a-zA-Z0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]{5,}$/;
    return regex.test(password);
  };

  const validateEmail = (email) => {
    const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    return regex.test(email);
  };

  const validatePhone = (phone) => {
    if (!phone) return true; // Телефон не обязателен
    const regex = /^\+\d{5,15}$/;
    return regex.test(phone);
  };

  const showError = (element, message) => {
    // Удаляем предыдущий tooltip если есть
    const existingTooltip = bootstrap.Tooltip.getInstance(element);
    if (existingTooltip) {
      existingTooltip.dispose();
    }

    // Добавляем атрибуты для tooltip
    element.setAttribute('data-bs-toggle', 'tooltip');
    element.setAttribute('data-bs-placement', 'bottom');
    element.setAttribute('title', message);

    // Инициализируем новый tooltip и показываем его
    const tooltip = new bootstrap.Tooltip(element);
    tooltip.show();

    element.classList.add('is-invalid');
  };

  const clearError = (element) => {
    const tooltip = bootstrap.Tooltip.getInstance(element);
    if (tooltip) {
      tooltip.dispose();
    }
    element.classList.remove('is-invalid');
  };


  username.addEventListener('input', () => {
    if (!validateUsername(username.value)) {
      showError(username, 'Username should contain only letters, numbers, and underscores, max 30 characters');
    } else {
      clearError(username);
    }
  });

  password.addEventListener('input', () => {
    if (!validatePassword(password.value)) {
      showError(password, 'Password should contain only letters, numbers, and special characters, min 5 characters');
    } else {
      clearError(password);
      // Проверяем совпадение паролей при изменении первого пароля
      if (passwordConfirm.value && password.value !== passwordConfirm.value) {
        showError(passwordConfirm, 'Passwords do not match');
      } else {
        clearError(passwordConfirm);
      }
    }
  });

  passwordConfirm.addEventListener('input', () => {
    if (password.value !== passwordConfirm.value) {
      showError(passwordConfirm, 'Passwords do not match');
    } else {
      clearError(passwordConfirm);
    }
  });

  email.addEventListener('input', () => {
    if (!validateEmail(email.value)) {
      showError(email, 'Please enter a valid email address');
    } else {
      clearError(email);
    }
  });

  phoneNumber.addEventListener('input', () => {
    if (!validatePhone(phoneNumber.value)) {
      showError(phoneNumber, 'Phone number should start with + and contain 5-15 digits');
    } else {
      clearError(phoneNumber);
    }
  });

  form.addEventListener('submit', (e) => {
    let isValid = true;

    // Валидация username
    if (!validateUsername(username.value)) {
      isValid = false;
      showError(username, 'Username should contain only letters, numbers, and underscores, max 30 characters');
    } else {
      clearError(username);
    }

    // Валидация password
    if (!validatePassword(password.value)) {
      isValid = false;
      showError(password, 'Password should contain only letters, numbers, and special characters');
    } else {
      clearError(password);
    }

    // Проверка совпадения паролей
    if (password.value !== passwordConfirm.value) {
      isValid = false;
      showError(passwordConfirm, 'Passwords do not match');
    } else {
      clearError(passwordConfirm);
    }

    // Валидация email
    if (!validateEmail(email.value)) {
      isValid = false;
      showError(email, 'Please enter a valid email address');
    } else {
      clearError(email);
    }

    // Валидация phone
    if (!validatePhone(phoneNumber.value)) {
      isValid = false;
      showError(phoneNumber, 'Phone number should start with + and contain 5-15 digits');
    } else {
      clearError(phoneNumber);
    }

    if (!isValid) {
      e.preventDefault();
    }
  });
});
