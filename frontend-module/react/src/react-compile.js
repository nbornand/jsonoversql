/** @jsx React.DOM */
var LikeButton = React.createClass({
  getInitialState: function() {
    return {items:[0,1,2,3], count : 0};
  },
  handleClick: function(event) {
      console.log('count');
      this.setState({count: this.state.count+1});
  },
  addItem: function(){
      var temp = this.state.items.splice(0,0, this.state.items.length);
      this.setState({items: this.state.items});
  },
  render: function() {
    console.log('render LikeButton')
    var text = this.state.liked ? 'like' : 'unlike';
    var self = this;
    return (
      <div style={this.style}>
        <button onClick={this.addItem}>Add item</button>
        { this.state.items.map(function(l){
            return <Cmp key={l} val={"link"+(l+self.state.count)} onClick={self.handleClick}></Cmp>
}) }
</div>
    );
  }
});

var DummyWrapper = React.createClass({displayName: 'W',
    render: function() {
        console.log('render DummyWrapper');
        var temp = <LikeButton />;
        console.log(temp);
        return temp;
    }
});


var Cmp = React.createClass({displayName: 'Cmp',
    handleClick: function(event) {
        console.log('count');
    },
  render: function() {
    console.log('render Cmp');
    return <p onClick={this.handleClick}> {this.props.val} </p>;
  }
});

React.renderComponent(
  <DummyWrapper />,
  document.getElementById('example')
);