const dateMode = {
  date: 1,
  dateTime: 2,
  bookingValue: 3
};

document.addEventListener('alpine:init', () => {
    document.title = messages.i18n_title
    let date = new Date();
    let dateFormatted = formatDate(date);
    let categoryTreeDepth = ${category_tree_depth};
    Alpine.data('modelData', () => ({
        id: 0,
        signum: false,
        amount: '',
        date: dateFormatted,
        valueDate: dateFormatted,
        time: formatTime(date),
        payee: '',
        account: 0,
        comment: '',
        categoryTreeDepth: categoryTreeDepth,
        categoryPath:  Array.from(Array(categoryTreeDepth), () => { return { id: 0 }; }),
        selectedTags: [],
        method: 0,
        number: '',
        resultText: '',
        resultCode: 0,
        data: ${data},
        transactions: [],
        async loadTransaction(transaction) {
            this.signum = transaction.amount > 0 ? true : false;
            this.amount = transaction.amount;
            this.payee = transaction.payee;
            this.comment = transaction.comment;
            this.number = transaction.number;
            this.date = transaction.date;
            this.time = transaction.time;
            this.valueDate = transaction.valueDate;
            if (transaction.category > 0) {
                let newPath = pathFromTerminalNode(this.data.categories, transaction.category)
                let lastIndex = newPath.length - 1
                for (let index = lastIndex; index >= 0; index--) {
                    this.categoryPath[lastIndex - index].id = newPath[index].id;
                    await this.$nextTick();
                }
            }
            this.selectedTags = transaction.tags;
            this.method = transaction.method
        },
        submitForm() {
            let data = {
                account: this.account,
                amount: this.signum ? this.amount : -this.amount,
                date: this.date,
                time: (currentDateMode(this.data, this.account) == dateMode.dateTime) ? this.time : null,
                valueDate: this.valueDate,
                payee: this.payee,
                category: terminalNodeFromPath(this.categoryPath),
                tags: this.selectedTags,
                comment: this.comment,
                method: this.method,
                number: this.number
            }
            let uri = "/transactions" + (this.id == 0 ? "" : ("/" + this.id))
            let method = id == 0 ? "POST" : "PUT"
            fetch(uri, {
                headers: {
                    'Content-Type': 'application/json'
                },
                method: method,
                body: JSON.stringify(data)
            }).then(response => {
                resultCode = response.status
                response.text().then(text => resultText = text)
            }).catch(errorHandler)
        },
        init() {
            this.account = this.data.accounts[0].id;
            this.$watch('amount', value => { if (value < 0) amount = -value } );
            ${categoryWatchers}
        }
    }))
})

function dateRowLabel(data, account) {
    switch(currentDateMode(data, account)) {
      case dateMode.date: return messages.i18n_date;
      case dateMode.dateTime: return messages.i18n_date + " / " + messages.i18n_time;
      case dateMode.bookingValue: return messages.i18n_booking_date + " / " + messages.i18n_value_date;
    }
}

function currentDateMode(data, account) {
    var type = accountType(data, account)
    if (!(type == "CASH")) {
      if (${withValueDate}) {
        return dateMode.bookingValue;
      }
    }
    return ${withTime} ? dateMode.dateTime : dateMode.date;
}

var errorHandler = (error) => alert('Error: ' + error)

function categoriesByParent(array, parent) {
    return array.filter(item => item.parent == parent)
}

function methodsForTypes(data, account, signum) {
    return data.methods.filter(item => item.accountTypes.includes(accountType(data, account)) && (item.type == 0 || item.type == (signum ? 1 : -1)))
}

function accountType(data, account) {
    return data.accounts.find(item => item.id == account).type
}

function accountCurrency(data, account) {
    return data.accounts.find(item => item.id == account).currency
}

function terminalNodeFromPath(path) {
  return path.reduce((acc, curr) => curr.id == 0 ? acc : curr.id, null);
}

function pathFromTerminalNode(array, category) {
    let result = [];
    let node = category;
    while(true) {
        result.push( { id: node } );
        node = array.find(item => item.id == node).parent;
        if (node == undefined) break;
    }
    return result
}

function formatPart(number) {
    return number.toString().padStart(2, '0')
}

function formatDate(date) {
  let month = formatPart(date.getMonth() + 1);
  let day = formatPart(date.getDate());
  let year = date.getFullYear();
  return [year, month, day].join('-');
}

function formatTime(date) {
  let hours = formatPart(date.getHours());
  let minutes = formatPart(date.getMinutes());
  return [hours, minutes].join(':');
}

function loadTransactions(account) {
    return fetch("/transactions?account_id="+account, {
            headers: {
                'Content-Type': 'application/json'
            },
            method: 'GET'
        });
}

function positionPopup(event, menu) {
    menu.style.top = event.pageY + "px"
    menu.style.left = event.pageX + "px"
}
