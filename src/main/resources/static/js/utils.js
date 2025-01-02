const alertTime = 5000;
let messageLevel = {ERROR: 'alert-danger', INFO: 'alert-info', SUCCESS: 'alert-success', WARNING: 'alert-warning'}
let popupContainer;
let visibleName;
let userId;
const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("value");
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("value");
let reviewsSection;
let categoryPath;
function get(name) {
  if ((name = (new RegExp('[?&]' + encodeURIComponent(name) + '=([^&]*)')).exec(location.search)))
    return decodeURIComponent(name[1]);
}

document.addEventListener("DOMContentLoaded", async () => {
  document.querySelector('form[action="/logout"]')?.addEventListener('submit', () => {
    sessionStorage.clear();
  });

  popupContainer = document.getElementById('popupContainer');
  document.getElementById('loginForm')?.addEventListener('submit', async (event) => {
    event.preventDefault();
    const formData = new FormData(event.target);
    try {
      const response = await fetch('/login', {
        method: 'POST',
        body: formData,
        headers: {
          [csrfHeader]: csrfToken
        }
      });
      if (response.ok) {
        location.reload();
      } else {
        createPopup('Login failed. Please check your credentials.', messageLevel.ERROR);
      }
    } catch (error) {
      createPopup('Error during login: ' + error, messageLevel.ERROR);
    }
  });
  if (!sessionStorage.getItem('currentUser')) {
    try {
      const response = await sendFetch(null, '/api/public/current-user', 'get');
      if (response.ok && !response.redirected) {
        if (response.user)
          sessionStorage.setItem('currentUser', JSON.stringify(response.user));
        else
          sessionStorage.removeItem('currentUser');
      }
    } catch (error) {
      createPopup('Error fetching user data: ' + error);
    }
  }
  let userData = JSON.parse(sessionStorage.getItem('currentUser'));
  if (userData) {
    visibleName = userData.visibleName;
    userId = userData.id;
    const headerUserAvatar = document.getElementById('headerUserAvatar');
    if (headerUserAvatar) {
      let avatar;
      if (userData.avatarPath) {
        avatar = document.createElement('img');
        avatar.src = '/images/' + userData.avatarPath;
        avatar.className = 'avatar-xs rounded-circle gallery-item';
      } else {
        avatar = generateAvatar(visibleName ?? userData.username, 'avatar-xs');
      }
      avatar.id = 'headerUserAvatar';
      headerUserAvatar.replaceWith(avatar)
    }
  }

  const searchResultsMenu = document.getElementById('searchResults');

  {
    const categoryBrandsData = JSON.parse(localStorage.getItem('categoryBrands'));
    if (categoryBrandsData)
      categoryBrands = new Map(categoryBrandsData);
  }
  const searchInput = document.getElementById('searchInput');

  searchInput?.addEventListener('input', async (event) => {
    const query = event.target.value.trim();
    if (!query || query.length < 3) return;
    try {
      const response = await sendFetch(null,
        `/api/public/search?query=${encodeURIComponent(query)}`, 'get');
      if (response.ok && !response.redirected) {
        displaySearchResults(response);
      } else {
        createPopup('Search failed', messageLevel.WARNING);
      }
    } catch (error) {
      createPopup('Error during search: ' + error, messageLevel.ERROR);
    }
  });
  searchInput?.addEventListener('keydown', async (event) => {
    const query = searchInput.value.trim();
    if (event.key === 'Enter' && query)
      location.href = '/search?query=' + query;
  });
  document.getElementById('mainSearchButton')?.addEventListener('click', () => {
    const query = searchInput.value.trim();
    if (query)
      location.href = '/search?query=' + query;
  })
  categoryPath = document.getElementById('categoryPath');
  reviewsSection = document.getElementById('reviewsSection');
  reviewsSection?.querySelectorAll('a:has(img.avatar-xs[src=""])').forEach(i => {
    const visibleName = i.textContent.trim();
    const avatar = generateAvatar(visibleName || "User", 'avatar-xs me-1');
    i.querySelector('img').replaceWith(avatar);
  });

  searchInput?.addEventListener('focusout', (event) => {
    if (!searchResultsMenu.contains(event.relatedTarget)) {
      searchResultsMenu.classList.remove('show');
    }
  });

  searchResultsMenu?.addEventListener('focusout', (event) => {
    if (!searchInput.contains(event.relatedTarget)) {
      searchResultsMenu.classList.remove('show');
    }
  });

  searchInput?.addEventListener('focus', () => {
    searchResultsMenu.classList.toggle('show', searchResultsMenu.innerHTML.trim() !== '');
  });

  function displaySearchResults(results) {
    searchResultsMenu.innerHTML = ''; // Очистка предыдущих результатов

    if (results.reviews.length > 0 || results.brands.length > 0 || results.categories.length > 0) {
      if (results.reviews.length > 0)
        searchResultsMenu.innerHTML += `<b class="dropdown-item-text">Reviews</b>`
      results.reviews.forEach(review => {
        const listItem = document.createElement('li');
        listItem.innerHTML = `<a class="dropdown-item" href="/review/${review.id}">${review.title}</a>`;
        searchResultsMenu.appendChild(listItem);
      });

      if (results.brands.length > 0)
        searchResultsMenu.innerHTML += `<b class="dropdown-item-text">Brands</b>`
      results.brands.forEach(brand => {
        const listItem = document.createElement('li');
        listItem.innerHTML = `<a class="dropdown-item" href="/brand/${brand.id}">${brand.name}</a>`;
        searchResultsMenu.appendChild(listItem);
      });

      if (results.categories.length > 0)
        searchResultsMenu.innerHTML += `<b class="dropdown-item-text">Categories</b>`
      results.categories.forEach(category => {
        const listItem = document.createElement('li');
        listItem.innerHTML = `<a class="dropdown-item" href="/category/${category.id}">${category.name}</a>`;
        searchResultsMenu.appendChild(listItem);
      });
    } else {
      searchResultsMenu.innerHTML = `<span class="dropdown-item">Nothing found...</span>`;
    }
    searchResultsMenu.classList.toggle('show', searchResultsMenu.innerHTML.trim() !== '');
  }

  loadLastUpdate().then(updates => {
    if (updates['categories']) {
      localStorage.removeItem('categories');
      localStorage.removeItem('categoryBrands');
    } else if (updates['brands']) {
      localStorage.removeItem('categoryBrands');
    }
    loadCategories().then(_ => {
      leafCategories = new Set(categories.keys());
      buildCategoryTree(categories);
      document.dispatchEvent(new Event('utilsInitiated'));
    });
  });
}, {once: true});

async function toggleBookmark(target, reviewId) {
  try {
    const reviewStatusKey = 'review_' + reviewId;
    const response = await sendFetch(null, `/api/toggle-bookmark/${reviewId}`, 'POST');
    if (response.ok) {
      let reviewStatus = sessionStorage.getItem(reviewStatusKey);
      if (!reviewStatus) await updateReviewStatus(reviewId);
      else {
        reviewStatus = JSON.parse(reviewStatus)
        reviewStatus.isBookmarked = response.isBookmarked;
        sessionStorage.setItem(reviewStatusKey, JSON.stringify(reviewStatus));
      }
      target.classList.toggle('text-warning', response.isBookmarked);
    }
  } catch (error) {
    createPopup('Error toggling bookmark: ' + error, messageLevel.ERROR);
  }
}

async function updateReviewStatus(reviewId) {
  try {
    if (!userId) return;
    const reviewStatusKey = 'review_' + reviewId;
    let reviewStatus = JSON.parse(sessionStorage.getItem(reviewStatusKey));
    if (!reviewStatus) {
      // Fetch from server if not cached
      const response = await sendFetch(null, `/api/review-status/${reviewId}`);
      if (response.ok) {
        sessionStorage.setItem(reviewStatusKey,
          JSON.stringify({
              isLiked: response.isLiked,
              isBookmarked: response.isBookmarked
            }
          ));
        reviewStatus = response;
      }
    }
    likeButton?.classList.toggle('text-danger', reviewStatus.isLiked);
    bookmarkButton?.classList.toggle('text-warning', reviewStatus.isBookmarked);
  } catch (error) {
    createPopup('Error loading review status: ' + error);
  }
}

function fillCategoryPath(categoryId) {
  if (!categoryPath) return;
  categoryPath.innerHTML = [categoryId, ...getAllParentCategories(categoryId)].reverse().map((id, i) => {
    const cat = categories.get(id);
    return `<span>${(i > 0 ? ' > ' : '')}
      <a class="text-decoration-none text-dark" href='/category/${cat.id}'>${cat.name}</a>
   </span>`
  }).join('');

}

function formatLikes(likes) {
  if (likes < 10000) {
    return likes.toString();
  } else if (likes < 1000000) {
    return (likes / 1000).toFixed(0) + 'K';
  } else if (likes < 1000000000) {
    return (likes / 1000000).toFixed(0) + 'M';
  } else {
    return (likes / 1000000000).toFixed(0) + 'B';
  }
}

let categories = new Map();
let categoryBrands = new Map();
let nodeMap = new Map();
let leafCategories;

/*
function buildCategoryTree(categories) {
  const categoryTree = new Map();
  // Инициализация корневых категорий
  categories.forEach((category, id) => {
    if (category.parentId === 0) {
      categoryTree.set(id, {supercategories: new Set([0]), children: new Map()});
    }
    leafCategories.delete(category.parentId);
  });

  // Рекурсивная функция добавления узлов
  function addCategoryToTree(id, category, parentTree) {
    if (parentTree.has(category.parentId)) {
      let supertcats = parentTree.get(category.parentId).supercategories.union(new Set([category.parentId]))
      parentTree.get(category.parentId).children.set(id,{
        supercategories: supertcats,
        children: new Map()
      });
      if(leafCategories.has(id))
        leafCategories.set(id,supertcats);
    } else {
      for (const node of parentTree.values()) {
        addCategoryToTree(id, category, node.children);
      }
    }
  }


  // Построение дерева
  categories.forEach((category, id) => {
    if (category.parentId !== 0) {
      addCategoryToTree(id, category, categoryTree);
    }
  });

  return categoryTree;
}
*/
function buildCategoryTree(categories) {
  const categoryTree = new Map();

  // Инициализация корневых категорий
  categories.forEach((category, id) => {
    if (category.parentId === 0) {
      const rootNode = {
        supercategories: new Set([0]),
        children: new Map()
      };
      categoryTree.set(id, rootNode);
      nodeMap.set(id, rootNode);
    }
    leafCategories.delete(category.parentId);
  });

  // Рекурсивная функция добавления узлов
  function addCategoryToTree(id, category, parentTree) {
    if (parentTree.has(category.parentId)) {
      const parentNode = parentTree.get(category.parentId);
      const supercategories = new Set(parentNode.supercategories);
      supercategories.add(category.parentId);

      const newNode = {
        supercategories: supercategories,
        children: new Map()
      };
      parentNode.children.set(id, newNode);
      nodeMap.set(id, newNode);
    } else {
      for (const node of parentTree.values()) {
        addCategoryToTree(id, category, node.children);
      }
    }
  }

  // Построение дерева
  categories.forEach((category, id) => {
    if (category.parentId !== 0) {
      addCategoryToTree(id, category, categoryTree);
    }
  });

}

function getAllDescendants(categoryId, maxDepth = 0) {
  const result = new Set([categoryId]);

  function collectChildren(node, depth) {
    if (maxDepth > 0 && depth >= maxDepth) return;  // Ограничение глубины, если maxDepth > 0

    for (const [childId, childNode] of node.children) {
      result.add(childId);
      collectChildren(childNode, depth + 1);  // Рекурсивно идем на уровень глубже
    }
  }

  const rootNode = nodeMap.get(categoryId);
  if (rootNode) {
    collectChildren(rootNode, 0);  // Начинаем с глубины 0
  }

  return result;
}


function DataURIToBlob(dataURI) {
  const splitDataURI = dataURI.split(',')
  const byteString = splitDataURI[0].indexOf('base64') >= 0 ? atob(splitDataURI[1]) : decodeURI(splitDataURI[1])
  const mimeString = splitDataURI[0].split(':')[1].split(';')[0]

  const ia = new Uint8Array(byteString.length)
  for (let i = 0; i < byteString.length; i++)
    ia[i] = byteString.charCodeAt(i)

  return new Blob([ia], {type: mimeString})
}

function mapUnionUpdate(map, ...iterables) {
  for (const iterable of iterables) {
    for (const item of iterable) {
      map.set(...item);
    }
  }
}

async function loadLastUpdate() {
  const savedLastUpdate = JSON.parse(localStorage.getItem('lastUpdate')) ?? {};
  let updateBooleans = {};
  let updateDates = {};
  try {
    const response = await sendFetch(null,
      '/api/public/get-update-timestamps', 'get');
    if (response.ok && !response.redirected) {
      Object.entries(response.updates).forEach(
        (kv) => {
          updateDates[kv[0]] = kv[1];
          updateBooleans[kv[0]] = (!savedLastUpdate[kv[0]] ||
            Date.parse(kv[1]) > Date.parse(savedLastUpdate[kv[0]]));
        });

      localStorage.setItem('lastUpdate', JSON.stringify(updateDates));
    } else {
      console.log('Failed loading updates', messageLevel.WARNING);
    }
  } catch (error) {
    createPopup('Error loading updates: ' + error, messageLevel.ERROR);
  }
  return updateBooleans;
}

async function loadCategories() {
  const cachedCategories = localStorage.getItem('categories');
  if (cachedCategories) {
    categories = new Map(JSON.parse(cachedCategories));
    return;
  }
  try {
    const response = await sendFetch(
      {categoryIds: Array.from(categories.keys())},
      '/api/public/get-categories', 'post');
    if (response.ok && !response.redirected) {
      response.categories.forEach(cat => categories.set(cat.id, cat));
      localStorage.setItem('categories', JSON.stringify(Array.from(categories.entries())));
    } else {
      createPopup('Failed loading categories', messageLevel.WARNING);
    }
  } catch (error) {
    createPopup('Error loading categories: ' + error, messageLevel.ERROR);
  }
}

async function getBrandsForCategories(categoriesToQuery) {
  try {
    const cachedBrands = new Map(JSON.parse(localStorage.getItem('categoryBrands')) || []);
    let uncachedBrands = [];
    // Check if brands for the category are already cached
    categoriesToQuery.forEach(id => { // Use forEach directly on the Set
      if (!cachedBrands.has(id))
        uncachedBrands.push(id);
    });
    if (uncachedBrands.length === 0) return;
    const response = await sendFetch({categoryIds: uncachedBrands},
      '/api/public/get-brands', 'POST');

    if (response.ok && !response.redirected) {
      console.log(response);
      response.brands.forEach(b => {
        if (!categoryBrands.has(b.categoryId))
          categoryBrands.set(b.categoryId, []);
        categoryBrands.get(b.categoryId).push(b.brand);
      })
      localStorage.setItem('categoryBrands', JSON.stringify(Array.from(categoryBrands.entries())));
    } else {
      createPopup('Failed to fetch brands', messageLevel.WARNING);
    }
  } catch (error) {
    createPopup('Error fetching brands: ' + error, messageLevel.ERROR);
  }
}

function getAllParentCategories(categoryId) {
  const parentCategories = [];
  let currentCategory = categories.get(categoryId);

  while (currentCategory && currentCategory.parentId !== 0) {
    parentCategories.push(currentCategory.parentId);
    currentCategory = categories.get(currentCategory.parentId);
  }

  return parentCategories;
}

function formatDateTime(datetime, includeTime = false) {
  if (!datetime) return '';
  const [date, time] = datetime.split('T');
  const [year, month, day] = date.split('-');
  const formattedDate = [day, month, year].join('.');

  if (!includeTime) return formattedDate;

  const timeStr = time.split('.')[0]; // убираем миллисекунды
  return `${formattedDate} ${timeStr}`;
}

function getRelativeTime(dateStr) {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now - date;
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);
  const diffWeek = Math.floor(diffDay / 7);
  const diffMonth = Math.floor(diffDay / 30);
  const diffYear = Math.floor(diffDay / 365);

  if (diffYear > 0) {
    return `${diffYear} ${diffYear === 1 ? 'year' : 'years'} ago`;
  }
  if (diffMonth > 0) {
    return `${diffMonth} ${diffMonth === 1 ? 'month' : 'months'} ago`;
  }
  if (diffWeek > 0) {
    return `${diffWeek} ${diffWeek === 1 ? 'week' : 'weeks'} ago`;
  }
  if (diffDay > 0) {
    return `${diffDay} ${diffDay === 1 ? 'day' : 'days'} ago`;
  }
  if (diffHour > 0) {
    return `${diffHour} ${diffHour === 1 ? 'hour' : 'hours'} ago`;
  }
  if (diffMin > 0) {
    return `${diffMin} ${diffMin === 1 ? 'minute' : 'minutes'} ago`;
  }
  return 'Just now';
}


function capitalizeFirstLetter(string) {
  return string[0].toUpperCase() + string.slice(1);
}

function generateAvatar(username, roleClass, round = true) {
  const avatarElement = document.createElement('div');
  const initials = username.split(' ').map(name => name.charAt(0).toUpperCase()).join('').slice(0, 2);
  const hue = username.hashCode() % 3600 / 10;
  const randomColor0 = `hsl(${hue}, 100%, 30%)`;
  const randomColor1 = `hsl(${hue}, 100%, 20%)`;
  avatarElement.className =
    `${round ? 'rounded-circle' : 'rounded-2'} flex-center text-white fw-bolder ${roleClass}`;
  avatarElement.style.background = `linear-gradient(0.4turn, ${randomColor0}, ${randomColor1})`;
  avatarElement.textContent = initials || '?';
  return avatarElement;
}

function createPopup(message, level, timeout) {
  const popup = document.createElement('div');
  popup.className = `alert ${level} alert-dismissible fade show d-flex justify-content-between align-items-center`;
  popup.role = 'alert';
  popup.innerHTML = `
                <span>${message}</span>
                <button type="button" class="float-end btn btn-close" data-dismiss="alert" aria-label="Close"> </button>
            `;
  popupContainer.appendChild(popup);
  switch (level) {
    case messageLevel.ERROR:
      console.error(message);
      break;
    case messageLevel.WARNING:
      console.warn(message);
      break;
    case messageLevel.INFO:
      console.info(message);
      break;
    case messageLevel.SUCCESS:
      console.log(message);
      break;
  }

  popup.getElementsByClassName("btn")[0]
    .addEventListener("click",
      () => {
        popup.classList.remove('show');
      }
    );

  popup.addEventListener('transitionend', () => {
    if (!popup.classList.contains('show')) popup.remove()
  });

  setTimeout(() => {
    popup.classList.remove('show');
  }, timeout ?? alertTime);
}

function sendFetch(data, action, method, type) {
  try {
    let status;
    let isFormData = (data instanceof FormData)
    if (isFormData && userId) data.append('userId', userId);
    let request = {
      method: method,
      headers: isFormData ? {[csrfHeader]: csrfToken} : {
        'Content-Type': type || 'application/json',
        [csrfHeader]: csrfToken
      }
    };
    if (method && method.toLowerCase() !== 'get') {
      request.body = isFormData || type ? data : JSON.stringify(data);
    }

    return fetch(action, request).then((response) => {
      console.log(response)
      status = response.status;
      try {
        if (response.ok && !response.redirected) {
          return response.json();
        } else if (!response.redirected) {
          return response.json();
        }
        console.log("Redirected");
        return {redirected: true, response: "Redirected: " + status};
      } catch (e) {
        return {response: "Error: " + status};
      }
    }).then((res) => {
      console.log(res);
      res.status = status;
      res.ok = status === 200;
      return res;
    });
  } catch (e) {
    console.error(e);
  }
}

function highlightBlock(element, highlightClass, timeout) {
  let classList = highlightClass.split(/ +/);
  classList.forEach(c => element.classList.add(c));
  setTimeout(() => classList.forEach(c => element.classList.remove(c)), timeout ?? 2000);
}

String.prototype.hashCode = function () {
  let hash = 0,
    i, chr;
  if (this.length === 0) return hash;
  for (i = 0; i < this.length; i++) {
    chr = this.charCodeAt(i);
    hash = ((hash << 5) - hash) + chr;
    hash |= 0;
  }
  return hash;
}

function processFile(file, max_height, max_width) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsArrayBuffer(file);

    reader.onload = function (event) {
      const blob = new Blob([event.target.result]);
      const blobURL = window.URL.createObjectURL(blob);

      const image = new Image();
      image.src = blobURL;

      image.onload = function () {
        const resized = resizeMe(image, max_height, max_width);
        resolve(resized);
      };

      image.onerror = function () {
        reject(new Error("Failed to load image"));
      };
    };

    reader.onerror = function () {
      reject(new Error("Failed to read file"));
    };
  });
}


// === RESIZE ====

function resizeMe(img, max_height, max_width) {

  const canvas = document.createElement('canvas');

  let width = img.width;
  let height = img.height;

  if (max_width && width > height) {
    if (width > max_width) {
      height = Math.round(height * max_width / width);
      width = max_width;
    } else return img.src;
  } else if (max_height) {
    if (height > max_height) {
      width = Math.round(width * max_height / height);
      height = max_height;
    } else return img.src;
  } else return img.src;

  console.log(height, width, max_height, max_width);
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext("2d");
  ctx.drawImage(img, 0, 0, width, height);
  return canvas.toDataURL("image/jpeg", 0.8);

}

