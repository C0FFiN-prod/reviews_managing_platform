// Симуляция категорий
// const categories = new Map([
//   [1, {id: 1, name: 'Health', parentId:      0},],
//   [2, {id: 2, name: 'Food', parentId:        0},],
//   [3, {id: 3, name: 'Electronic', parentId: 0},],
//   [4, {id: 4, name: 'Dairy', parentId:       2},],
//   [5, {id: 5, name: 'Backery', parentId:     2},],
//   [6, {id: 6, name: 'Drink', parentId:      2},],
//   [7, {id: 7, name: 'Soft drink', parentId: 6},],
//   [8, {id: 8, name: 'Smartphone', parentId: 3},],
//   [9, {id: 9, name: 'Peripheral', parentId:  3},],
//   [10,{id:10, name: 'Monitor', parentId:    9},],
//   [11,{id:11, name: 'Keyboard', parentId:   9},],
// ]);
// let categoryBrands = new Map([
//   [1, {id:10,name: 'HellsCare Inc.'}],
//   [5, {id:1, name: 'OOO OwoceBaza' }],
//   [3 ,{id:2, name: 'Xiaomi'}],
//   [10,{id:4, name: 'ASUS'}],
//   [11,{id:17,name: 'BestKeys'}],
// ]);

let hasChanges = false;


let choices = {category: null, brand: null, model: "", rating: 0};
let review = {summary: "", text: "", pros: new Map(), cons: new Map()}
let brands = new Map();
let isCategoryChanged = false;


function updateBrands(categoryId) {
  brands.clear();
  if (categoryBrands.has(categoryId)) {
    categoryBrands.get(categoryId).forEach(br => brands.set(br.id, br.name));
  }

  leafCategories.get(categoryId).forEach(id => {
    if (categoryBrands.has(id)) {
      categoryBrands.get(id).forEach(br => brands.set(br.id, br.name));

    }
  })
}


let reviewContentTitle;
let reviewSubjectTitle;

function updateReviewTitle() {
  let title = [
    (categories.get(choices.category) || {name: ""}).name,
    (brands.get(choices.brand) || ""),
    choices.model
  ].join(' ').trim() || '&nbsp;';
  reviewContentTitle.innerHTML = title;
  reviewSubjectTitle.innerHTML = title
}

function handleInput(event, list, errorBlock, map) {
  hasChanges = true;
  const input = event.target;
  const inputValue = input.value.trim();
  input.classList.remove('is-invalid');
  errorBlock.classList.add('d-none');
  if (event.key === 'Enter' && inputValue) {
    if (inputValue.length < 4 || inputValue.length > 30) {
      input.classList.add('is-invalid');
      errorBlock.classList.remove('d-none');
      return;
    }
    let previousDeclaration = map.get(inputValue.toLowerCase());
    if (previousDeclaration) {
      highlightBlock(previousDeclaration.element, 'border-1 border-info');
      return;
    }

    const li = document.createElement('li');
    li.className = 'list-group-item d-flex align-items-center border-transition';

    const deleteBtn = document.createElement('button');
    deleteBtn.textContent = '';
    deleteBtn.className = 'btn btn-sm btn-close me-1';
    li.appendChild(deleteBtn);
    list.appendChild(li);

    const text = document.createTextNode(capitalizeFirstLetter(inputValue));
    map.set(inputValue.toLowerCase(), {value: capitalizeFirstLetter(inputValue), element: li});
    li.append(deleteBtn, text);
    list.appendChild(li);
    input.value = '';

    deleteBtn.addEventListener('click', () => {
      hasChanges = true;
      console.log(li)
      console.log(li.childNodes)
      map.delete(li.childNodes[1].textContent.toLowerCase());
      list.removeChild(li);
    });
  }
}


let reviewSubject;
let reviewContent;

let categoryInput;
let categoryError;
let categoryInputId;
let suggestionsList;

let brandSelect;
let brandError;

let modelName;
let modelError;

let rating;
let ratingError;

let shortReview;
let shortReviewError;

let pros;
let prosInput;
let prosError;

let cons;
let consInput;
let consError;

let reviewBody;
let galleryContainer;


function validateSubject() {
  let ok = true;
  if (!leafCategories.has(choices.category)) {
    categoryInput.classList.add('is-invalid');
    categoryError.classList.remove('d-none');
    ok = false;
  }
  if (!choices.brand) {
    brandSelect.classList.add('is-invalid');
    brandError.classList.remove('d-none');
    ok = false;
  }
  if (!choices.model) {
    modelName.classList.add('is-invalid');
    modelError.classList.remove('d-none');
    ok = false;
  }
  if (!choices.rating) {
    rating.classList.add('is-invalid');
    ratingError.classList.remove('d-none');
    ok = false;
  }
  return ok;
}

function validateContent() {
  let ok = true;
  review.summary = shortReview.textContent.trim();
  review.text = reviewBody.textContent.trim();
  let textLength = review.summary.length;
  if (textLength && (textLength < 4 || textLength > 50)) {
    shortReview.classList.add('is-invalid');
    shortReviewError.classList.remove('d-none');
    ok = false;
  }
  textLength = review.text.length;
  if (textLength < 100) {
    createPopup('Review length must be more than 100 characters', messageLevel.ERROR, 3000);
    highlightBlock(reviewBody, 'border-danger', 3000);
    ok = false;
  }
  return ok;
}

function saveAsDraft() {
  sendReviewData('draft').then(_ => {
  });
}

function publishReview() {
  sendReviewData('publish').then(_ => {
  });
}

async function sendReviewData(action) {
  let formData = collectReviewData();
  if (!formData) return;
  formData.append('action', action);
  try {
    const response = await sendFetch(formData, "/api/save-review", "POST");

    if (response.ok) {
      createPopup("Review submitted successfully!", messageLevel.SUCCESS);
    } else {
      createPopup(response.response, messageLevel.ERROR);
    }
  } catch (error) {
    createPopup("Error:" + error.toString(), messageLevel.ERROR);
  }
}

function collectReviewData() {

  if (!validateSubject()) {
    reviewSubject.classList.remove('d-none');
    reviewContent.classList.add('d-none');
    return;
  }
  if (!validateContent()) {
    reviewSubject.classList.add('d-none');
    reviewContent.classList.remove('d-none');
    return;
  }

  const formData = new FormData();

  // Сбор данных из первой секции
  if (reviewId)
    formData.append("reviewId", reviewId);
  formData.append("categoryId", choices.category);
  formData.append("brandId", choices.brand);
  formData.append("modelName", choices.model);
  formData.append("shortReview", shortReview.value);
  formData.append("rating", choices.rating);
  console.log(JSON.stringify(Object.fromEntries(formData.entries())));
  // Сбор списка плюсов и минусов
  formData.append("pros", JSON.stringify(Array.from(review.pros.values(), i => i.value)));
  formData.append("cons", JSON.stringify(Array.from(review.cons.values(), i => i.value)));
  console.log(JSON.stringify(Object.fromEntries(formData.entries())));

  // Сбор содержимого редактора
  let imagesNewId2Ind = {};
  let imagesId2NewId = {};
  let index = 0;
  Object.entries(imagesInd2Id).forEach(([i, id]) => {
    id = parseInt(id, 10);
    if (imagesIdCount[id] > 0) {
      let newId = imagesId2NewId[id];
      if (imagesNewId2Ind[newId] === undefined) {
        newId = index;
        imagesId2NewId[id] = index++;
        imagesNewId2Ind[newId] = [];
      }
      imagesNewId2Ind[newId].push(Number(i));
    }
  })
  console.log(imagesNewId2Ind);
  console.log(imagesId2NewId);

  const reviewBodyClone = reviewBody.cloneNode(true);
  // Заменяем картинки в клоне
  reviewBodyClone.querySelectorAll("img[data-img-key]").forEach((img, _) => {
    let index = img.dataset.imgIndex;
    let id = imagesInd2Id[index];
    id = parseInt(id, 10);
    if (id !== undefined && imagesId2NewId[id] !== undefined) {
      const marker = document.createTextNode(`[[IMAGE:${index}]]`);
      img.replaceWith(marker);
    } else
      img.remove();
  });

  formData.append('reviewBody', reviewBodyClone.innerHTML.trim());
  console.log(JSON.stringify(Object.fromEntries(formData.entries())));

  formData.append('positions', JSON.stringify(imagesNewId2Ind));

  Object.entries(imagesId2Src).forEach(([id, src]) => {
    id = parseInt(id, 10);
    if (imagesIdCount[id] > 0) {
      const i = src.search('/images/');
      if (i !== -1) {
        formData.append('images', new Blob([''], {type: 'text/plain'}), imagesId2NewId[id] + '_' + src.slice(i + 8));
      } else
        formData.append('images', DataURIToBlob(src), `${imagesId2NewId[id]}_${src.hashCode()}.jpg`);
    }
  });
  console.log(JSON.stringify(Object.fromEntries(formData.entries())));


  return formData;
}


function formatText(command) {
  document.execCommand(command, false, null);
}

let imageIndex = 0;
let imageKey = 0;
const imagesInd2Id = {};
const imagesSrc2Id = {};
const imagesId2Src = {};
const imagesIdCount = {};
const imageGallery = []; // Массив для хранения загруженных изображений


// Функция для отображения изображений в галерее
function renderGallery() {

  galleryContainer.innerHTML = ''; // Очищаем контейнер

  // Создаем элементы для каждой картинки в галерее
  imageGallery.forEach((id, index) => {
    const imgElement = document.createElement('div');
    imgElement.className = 'gallery-item position-relative';
    imgElement.innerHTML = `
      <img src="${imagesId2Src[id]}" data-img-key="${id}" class="border-transition gallery-item img-thumbnail" alt="image"/>
      <button class="btn btn-close btn-close-white bg-light border-dark position-absolute top-0 end-0" onclick="removeImageFromGallery(${index})"></button>
    `;
    galleryContainer.appendChild(imgElement);

    // При клике на изображение из галереи вставляем его в редактор
    imgElement.getElementsByTagName('img')[0].addEventListener('click', () => {
      insertImageAtCursor(id);
    });
  });
  if (!galleryContainer.innerHTML)
    galleryContainer.innerHTML = `<h5 class="text-center">For now, it's empty</h5>`
}

// Функция для удаления изображения из галереи
function removeImageFromGallery(index) {
  let id = imageGallery[index];
  if (imagesIdCount[id] > 0) {
    createPopup('This image is still used in text', messageLevel.WARNING);
    let img = galleryContainer.querySelector(`img[data-img-key="${id}"]`);
    highlightBlock(img, 'border-warning', alertTime)
    return;
  }
  hasChanges = true;
  imageGallery.splice(index, 1); // Удаляем изображение из массива
  renderGallery(); // Перерисовываем галерею
  deleteImageIfUnused(id);
}

function deleteImageIfUnused(id) {
  if (imagesIdCount[id] <= 0 && imageGallery.indexOf(id) === -1) {
    hasChanges = true;
    let hash = imagesId2Src[id].hashCode();
    delete imagesIdCount[id];
    delete imagesId2Src[id];
    delete imagesSrc2Id[hash];
  }
}

function insertImageAtCursor(id) {
  const selection = window.getSelection();
  const range = selection.rangeCount > 0 ? selection.getRangeAt(0) : null;
  if (range && reviewBody.contains(range.startContainer)) {
    hasChanges = true;
    const img = document.createElement('img');
    img.src = imagesId2Src[id];
    img.className = 'img review-img';
    img.dataset.imgKey = id;
    img.dataset.imgIndex = imageIndex.toString();
    imagesInd2Id[imageIndex] = id;
    imageIndex++;
    // Вставляем изображение и перенос строки
    range.insertNode(img);

    // Ставим курсор после изображения
    range.setStartAfter(img);
    range.collapse(true);
    selection.removeAllRanges();
    selection.addRange(range);
  } else {
    createPopup('Please click inside the review area before adding an image.', messageLevel.INFO);
  }
}

const reviewId = window.location.pathname.startsWith('/edit') ?
  parseInt(window.location.pathname.replace('/edit-review/', ''), 10) : null;

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('saveAsDraftBtn').addEventListener('click', saveAsDraft);
  document.getElementById('publishReviewBtn').addEventListener('click', publishReview);
  pros = document.getElementById('pros');
  cons = document.getElementById('cons');
  prosInput = document.getElementById('prosInput');
  consInput = document.getElementById('consInput');
  prosError = document.getElementById('prosError');
  consError = document.getElementById('consError');
  categoryInput = document.getElementById('category');
  categoryInputId = document.getElementById('categoryId');
  suggestionsList = document.getElementById('categorySuggestions');
  reviewContent = document.getElementById('reviewContent');
  reviewSubject = document.getElementById('reviewSubject');
  brandSelect = document.getElementById('brandSelect');
  brandError = document.getElementById('brandError');
  reviewContentTitle = document.getElementById('reviewContentTitle');
  reviewSubjectTitle = document.getElementById('reviewSubjectTitle');
  modelName = document.getElementById('modelName');
  modelError = document.getElementById('modelError');
  categoryError = document.getElementById('categoryError');
  rating = document.getElementById('rating');
  ratingError = document.getElementById('ratingError');
  shortReview = document.getElementById('shortReview');
  shortReviewError = document.getElementById('shortReviewError');
  reviewBody = document.getElementById('reviewBody');
  galleryContainer = document.getElementById('galleryContainer');

  function collectListItemsToMap(list) {
    let map = new Map();
    Array.from(list.children).forEach(i => {
      i.querySelector('button').addEventListener('click', () => {
        hasChanges = true;
        console.log(i)
        console.log(i.childNodes)
        map.delete(i.childNodes[1].textContent.toLowerCase());
        list.removeChild(i);
      });
      let value = i.textContent.trim();
      map.set(value.toLowerCase(), {element: i, value: capitalizeFirstLetter(value)});
    });
    return map;
  }

  if (reviewId) {
    choices = {
      category: parseInt(categoryInputId.value),
      brand: parseInt(brandSelect.value),
      model: modelName.value,
      rating: parseInt(document.querySelector('input[name="rating"]:checked').value),
    };
    review = {
      summary: shortReview.value,
      text: reviewBody.innerHTML,
      pros: collectListItemsToMap(pros),
      cons: collectListItemsToMap(cons)
    }
    galleryContainer.querySelectorAll('img').forEach(img => {
      imageKey++;
      const id = parseInt(img.dataset.imgKey, 10);
      imageGallery.push(id);
      imagesSrc2Id[img.src.hashCode()] = id;
      imagesId2Src[id] = img.src;
      imagesIdCount[id] = 0;
    });
    reviewBody.querySelectorAll('img').forEach(img => {
      const index = parseInt(img.dataset.imgIndex, 10);
      const id = imagesSrc2Id[img.src.hashCode()];
      imagesIdCount[id]++;
      imagesInd2Id[index] = id;
      img.dataset.imgKey = id;
      imageIndex = Math.max(imageIndex, index);
    });
    imageIndex++;
    document.getElementById('confirmModal-body').innerHTML =
      `<p>Are you sure you want to delete this review? This action cannot be undone.</p>`;

    document.getElementById('confirmModal-confirm-btn').addEventListener('click', async () => {
      const result = await sendFetch({reviewId: reviewId}, `/api/delete-review`, 'POST');
      if (result && result.status === 200) {
        createPopup(result.response, messageLevel.SUCCESS, alertTime);
        setTimeout(() => window.location.href = '/home', 3000);
      } else
        createPopup(result.response, messageLevel.ERROR, alertTime);
    }, {once: true});
  }


  shortReview.addEventListener('input', () => {
    shortReview.classList.remove('is-invalid');
    shortReviewError.classList.add('d-none');
    review.summary = shortReview.value;
  });

  const ratingRadios = reviewSubject.querySelectorAll('input[name="rating"]');
  ratingRadios.forEach(radio => {
    radio.addEventListener("click", () => {
      if (radio.checked) {
        hasChanges = true;
        choices.rating = radio.value;
        rating.classList.remove('is-invalid');
        ratingError.classList.add('d-none');
      }
    });
  });


  // category search


  reviewSubject.addEventListener('focus', (event) => {
    if (event.target !== suggestionsList && event.target !== categoryInput) {
      suggestionsList.classList.remove('show');
    }
  }, {capture: true});
  reviewSubject.addEventListener('click', (event) => {
    if (event.target !== suggestionsList && event.target !== categoryInput) {
      suggestionsList.classList.remove('show');
    }
  }, {capture: true});
  categoryInput.addEventListener('focus', () => {
    categoryInput.classList.remove('is-invalid');
    categoryError.classList.add('d-none');
    suggestionsList.classList.remove('d-none');
    if (suggestionsList.children.length)
      suggestionsList.classList.add('show');
  });

  categoryInput.addEventListener('transitionend', () => {
    if (!suggestionsList.classList.contains('show'))
      suggestionsList.classList.add('d-none');
  });

  categoryInput.addEventListener('input', (event) => {
    hasChanges = true;
    categoryInput.classList.remove('is-invalid');
    categoryError.classList.add('d-none');
    const query = categoryInput.value.trim().toLowerCase();
    suggestionsList.innerHTML = '';
    choices.category = null
    if (query.length === 0) {
      suggestionsList.classList.remove('show');
      return;
    }

    const filtered = categories.keys().filter(id => {
      const cat = categories.get(id);
      return (!choices.category || !leafCategories.has(id) || cat.name.toLowerCase() !== query) &&
        (cat.name.toLowerCase().includes(query) ||
          (categories.get(cat.parentId) ?? {name: ''}).name.toLowerCase().includes(query))
    });

    if (filtered.length === 0) {
      suggestionsList.classList.remove('show');
      suggestionsList.innerHTML = '';
    } else {
      suggestionsList.classList.remove('d-none');
      suggestionsList.classList.add('show');

      filtered.forEach(id => {
        const cat = categories.get(id);
        const item = document.createElement('li');
        item.className = 'list-group-item';
        item.textContent = (cat.parentId ? categories.get(cat.parentId).name + ' > ' : "") + cat.name;
        item.dataset.id = cat.id;

        item.addEventListener('click', () => {
          categoryInput.classList.remove('is-invalid');
          categoryError.classList.add('d-none');
          categoryInput.value = cat.name;
          isCategoryChanged = choices.category !== cat.id;
          choices.category = cat.id;
          let isLeafCategory;
          if (leafCategories.has(cat.id)) {
            suggestionsList.classList.remove('show');
            isLeafCategory = true;
            categoryInputId.value = cat.id;
            suggestionsList.innerHTML = '';
            updateReviewTitle();
            brandSelect.dispatchEvent(new Event('click'));
          } else {
            suggestionsList.classList.add('show');
            isLeafCategory = false;
          }
          if (!isLeafCategory && filtered.length !== 0)
            event.target.dispatchEvent(new Event('input'));
        });

        suggestionsList.appendChild(item);
      });
    }
  });

  // brand selection
  brandSelect.addEventListener('click', async () => {
    // Reset brand selection
    hasChanges = true;
    choices.brand = Number(brandSelect.value);
    brandSelect.innerHTML = '<option value="" selected disabled class="d-none">Select a brand...</option>';
    brandSelect.classList.remove('is-invalid');
    brandError.classList.add('d-none');
    let categoryId = choices.category;
    if (!categoryId || !leafCategories.has(categoryId)) {
      categoryInput.focus();
      categoryInput.classList.add('is-invalid');
      categoryError.classList.remove('d-none');
      return;
    }

    try {
      if (isCategoryChanged) {
        console.log('Fetching brands...');
        await getBrandsForCategories([categoryId, ...leafCategories.get(categoryId).supercategories.keys()]);
        isCategoryChanged = false;
      }
      updateBrands(categoryId);
      if (brands.size === 0) {
        brandSelect.innerHTML = '<option value="" selected disabled>No brands available</option>';
        return;
      }
      // Populate the brand select field
      brands.forEach((name, id) => {
        const option = document.createElement('option');
        option.value = id;
        option.textContent = name;
        brandSelect.appendChild(option);
      });
      if (brands.has(choices.brand)) {
        brandSelect.value = choices.brand;
      } else {
        choices.brand = null;
      }
      updateReviewTitle();
    } catch (error) {
      brandSelect.innerHTML = '<option value="" disabled>Error loading brands</option>';
      console.log(error);
    }
  });

  modelName.addEventListener('input', (event) => {
    modelName.classList.remove('is-invalid');
    modelError.classList.add('d-none');
    choices.model = event.target.value;
    updateReviewTitle();
  });
  // Form Validation
  reviewSubject.querySelector('button[name="next"]').addEventListener('click', (event) => {
    event.preventDefault();
    if (!validateSubject()) return;
    reviewContent.classList.remove('d-none');
    reviewSubject.classList.add('d-none');
  });
  reviewContent.querySelector('button[name="back"]').addEventListener('click', () => {
    reviewContent.classList.add('d-none');
    reviewSubject.classList.remove('d-none');
  })

  prosInput.addEventListener('keydown', (e) => handleInput(e, pros, prosError, review.pros));
  consInput.addEventListener('keydown', (e) => handleInput(e, cons, consError, review.cons));

  document.getElementById('boldBtn').addEventListener('click', () => {
    document.execCommand('bold');
  });

  document.getElementById('italicBtn').addEventListener('click', () => {
    document.execCommand('italic');
  });

  document.getElementById('imageBtn').addEventListener('click', () => {
    document.getElementById('imageInput').click();
  });

// Обработчик загрузки изображений
  document.getElementById('imageInput').addEventListener('change', async (event) => {
    const file = event.target.files[0];
    if (file) {
      console.log('uploaded');
      processFile(file, 500)
        .then(imgSrc => {
          const srcHash = imgSrc.hashCode();
          if (imagesSrc2Id[srcHash] === undefined) {
            imagesId2Src[imageKey] = imgSrc;
            imagesSrc2Id[srcHash] = imageKey;
            imagesIdCount[imageKey] = 0;
            imageGallery.push(imageKey); // Добавляем изображение в галерею
            imageKey++;
            renderGallery(); // Перерисовываем галерею
          } else console.log('Image already uploaded');
        })
        .catch(error => {
          console.error("Error:", error);
        });


    }

    // Сбросить значение поля input, чтобы событие change сработало при повторном выборе того же файла
    event.target.value = '';
  });
  renderGallery();


  // Настройка наблюдателя
  const observer = new MutationObserver((mutationsList) => {
    for (const mutation of mutationsList) {
      if (mutation.type === 'childList') {
        mutation.removedNodes.forEach(node => {
          if (node.tagName === 'IMG') {
            let id = Number(node.dataset.imgKey);
            let index = Number(node.dataset.imgIndex);
            imagesIdCount[id]--;
            delete imagesInd2Id[index];
            deleteImageIfUnused(id);
            console.log(`Erased image ${node.dataset.imgKey}`)
          }
        });
        mutation.addedNodes.forEach(node => {
          if (node.tagName === 'IMG') {
            let id = Number(node.dataset.imgKey ?? ++imageKey);
            let index = Number(node.dataset.imgIndex ?? ++imageIndex);
            if (!imagesId2Src[id]) {
              let src = node.src;
              node.dataset.imgKey = id.toString();
              node.dataset.imgIndex = index.toString();
              imagesSrc2Id[src.hashCode()] = id;
              imagesId2Src[id] = src;
              imagesIdCount[id] = 0;
              imageGallery.push(id);
              renderGallery();
            }
            imagesIdCount[id]++;
            imagesInd2Id[index] = id;

            console.log(`Added image ${node.dataset.imgKey}`)
          }
        });
      }
    }
  });


// Конфигурация и запуск наблюдения
  observer.observe(reviewBody, {childList: true, subtree: true});


});